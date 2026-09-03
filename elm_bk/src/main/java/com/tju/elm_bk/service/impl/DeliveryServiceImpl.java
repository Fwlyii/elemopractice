package com.tju.elm_bk.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tju.elm_bk.constant.DeliveryTaskStatus;
import com.tju.elm_bk.constant.OrderStatus;
import com.tju.elm_bk.constant.RiderAuditStatus;
import com.tju.elm_bk.dto.DeliveryExceptionCreateDTO;
import com.tju.elm_bk.dto.DeliveryExceptionResolveDTO;
import com.tju.elm_bk.entity.*;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.*;
import com.tju.elm_bk.service.DeliveryService;
import com.tju.elm_bk.service.AssetService;
import com.tju.elm_bk.utils.SecurityUtils;
import com.tju.elm_bk.vo.DeliveryExceptionVO;
import com.tju.elm_bk.vo.DeliveryTaskVO;
import com.tju.elm_bk.websocket.WebSocketServer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class DeliveryServiceImpl implements DeliveryService {
    private final DeliveryTaskMapper deliveryTaskMapper;
    private final RiderMapper riderMapper;
    private final OrdersMapper ordersMapper;
    private final OrderStatusHistoryMapper historyMapper;
    private final UserMapper userMapper;
    private final BusinessMapper businessMapper;
    private final NotificationMapper notificationMapper;
    private final WebSocketServer webSocketServer;
    private final AssetMapper assetMapper;
    private final FoodMapper foodMapper;
    private final AssetService assetService;

    public DeliveryServiceImpl(DeliveryTaskMapper deliveryTaskMapper,
                               RiderMapper riderMapper,
                               OrdersMapper ordersMapper,
                               OrderStatusHistoryMapper historyMapper,
                               UserMapper userMapper,
                               BusinessMapper businessMapper,
                               NotificationMapper notificationMapper,
                               WebSocketServer webSocketServer,
                               AssetMapper assetMapper,
                               FoodMapper foodMapper,
                               AssetService assetService) {
        this.deliveryTaskMapper = deliveryTaskMapper;
        this.riderMapper = riderMapper;
        this.ordersMapper = ordersMapper;
        this.historyMapper = historyMapper;
        this.userMapper = userMapper;
        this.businessMapper = businessMapper;
        this.notificationMapper = notificationMapper;
        this.webSocketServer = webSocketServer;
        this.assetMapper = assetMapper;
        this.foodMapper = foodMapper;
        this.assetService = assetService;
    }

    @Override
    public List<DeliveryTaskVO> listAvailableTasks() {
        User rider = requireApprovedRider(true);
        List<DeliveryTaskVO> tasks = deliveryTaskMapper.listAvailable();
        tasks.forEach(this::hideCustomerPrivacyBeforeAccept);
        return tasks;
    }

    @Override
    public List<DeliveryTaskVO> listMyTasks(Boolean active) {
        User rider = requireApprovedRider(false);
        return deliveryTaskMapper.listByRider(rider.getId(), active);
    }

    @Override
    public DeliveryTaskVO getTask(Long taskId) {
        User current = currentUser();
        DeliveryTask task = requireTask(taskId);
        assertTaskVisibleTo(current, task);
        return deliveryTaskMapper.selectViewById(taskId);
    }

    @Override
    public DeliveryTaskVO getOrderDelivery(Long orderId) {
        User current = currentUser();
        Order order = requireOrder(orderId);
        DeliveryTask task = deliveryTaskMapper.selectByOrderId(orderId);
        if (task == null) {
            return null;
        }
        assertOrderVisibleTo(current, order, task);
        return deliveryTaskMapper.selectViewByOrderId(orderId);
    }

    @Override
    @Transactional
    public DeliveryTaskVO merchantAcceptOrder(Long orderId) {
        User merchant = currentUser();
        Order order = requireOrder(orderId);
        requireBusinessOwner(merchant.getId(), order.getBusinessId());
        changeOrderState(orderId, OrderStatus.WAITING_MERCHANT_ACCEPT, OrderStatus.WAITING_DISPATCH,
                merchant.getId(), "商家接单，进入制作中");
        notifyUser(order.getCustomerId(), "商家已接单，正在制作餐品", orderId);
        return null;
    }

    @Override
    @Transactional
    public DeliveryTaskVO merchantReadyOrder(Long orderId) {
        User merchant = currentUser();
        Order order = requireOrder(orderId);
        requireBusinessOwner(merchant.getId(), order.getBusinessId());
        if ("PICKUP".equalsIgnoreCase(order.getServiceMode())) {
            // 自取单不进入骑手配送域；商家确认出餐后直接进入“待自取”。
            changeOrderState(orderId, OrderStatus.WAITING_DISPATCH, OrderStatus.WAITING_PICKUP,
                    merchant.getId(), "商家确认出餐，等待顾客到店自取");
            notifyUser(order.getCustomerId(), "餐品已备好，请到店取餐", orderId);
            return null;
        }
        if (deliveryTaskMapper.selectByOrderId(orderId) != null) throw new APIException("该订单已经生成配送任务");
        changeOrderState(orderId, OrderStatus.WAITING_DISPATCH, OrderStatus.WAITING_RIDER_ACCEPT,
                merchant.getId(), "商家确认出餐，配送任务进入接单大厅");
        DeliveryTask task = new DeliveryTask(); task.setOrderId(orderId); task.setTaskStatus(DeliveryTaskStatus.WAITING_RIDER.name());
        task.setDistanceKm(estimateDistance(orderId)); task.setRiderFee(order.getDeliveryPrice() == null ? BigDecimal.ZERO : order.getDeliveryPrice());
        deliveryTaskMapper.insertTask(task);
        notifyUser(order.getCustomerId(), "餐品已出餐，正在等待骑手接单", orderId);
        broadcastDeliveryUpdate("新配送任务 #" + task.getId() + " 等待骑手接单", orderId);
        return deliveryTaskMapper.selectViewById(task.getId());
    }

    @Override
    @Transactional
    public void merchantRejectOrder(Long orderId) {
        User merchant = currentUser();
        Order order = requireOrder(orderId);
        requireBusinessOwner(merchant.getId(), order.getBusinessId());
        changeOrderState(orderId, OrderStatus.WAITING_MERCHANT_ACCEPT, OrderStatus.CANCELLED,
                merchant.getId(), "商家拒绝订单");
        foodMapper.restoreStockByOrder(orderId);
        assetMapper.releaseCouponByOrder(orderId);
        ordersMapper.updatePaymentStatus(orderId, "REFUNDED");
        if (Boolean.TRUE.equals(order.getWalletPaid()) || (order.getPointsUsed() != null && order.getPointsUsed() > 0)) {
            assetService.refundOrderAssets(orderId, order.getCustomerId(), order.getPointsUsed(),
                    Boolean.TRUE.equals(order.getWalletPaid()) ? order.getOrderTotal() : BigDecimal.ZERO);
        }
        notifyUser(order.getCustomerId(), "商家未接受订单，订单已取消", orderId);
    }

    @Override
    @Transactional
    public DeliveryTaskVO acceptTask(Long taskId) {
        User rider = requireApprovedRider(true);
        DeliveryTask task = requireTask(taskId);
        requireTransition(task, DeliveryTaskStatus.ACCEPTED);
        if (deliveryTaskMapper.acceptTask(taskId, rider.getId()) != 1) {
            throw new APIException("任务已被其他骑手抢走，请刷新任务大厅");
        }
        changeOrderState(task.getOrderId(), OrderStatus.WAITING_RIDER_ACCEPT, OrderStatus.WAITING_PICKUP,
                rider.getId(), "骑手已接单");
        Order order = requireOrder(task.getOrderId());
        notifyOrderParties(order, rider.getId(), "骑手已接单，正在前往商家", task.getOrderId());
        return deliveryTaskMapper.selectViewById(taskId);
    }

    @Override
    @Transactional
    public DeliveryTaskVO arriveStore(Long taskId) {
        User rider = requireApprovedRider(false);
        DeliveryTask task = requireOwnedTask(taskId, rider.getId());
        requireTransition(task, DeliveryTaskStatus.ARRIVED_STORE);
        if (deliveryTaskMapper.markArrivedStore(taskId, rider.getId()) != 1) {
            throw new APIException("只有已接单任务才能确认到店");
        }
        Order order = requireOrder(task.getOrderId());
        notifyOrderParties(order, rider.getId(), "骑手已到店，正在等待取餐", task.getOrderId());
        return deliveryTaskMapper.selectViewById(taskId);
    }

    @Override
    @Transactional
    public DeliveryTaskVO pickup(Long taskId) {
        User rider = requireApprovedRider(false);
        DeliveryTask task = requireOwnedTask(taskId, rider.getId());
        requireTransition(task, DeliveryTaskStatus.DELIVERING);
        if (deliveryTaskMapper.markPickedUp(taskId, rider.getId()) != 1) {
            throw new APIException("骑手必须先确认到店才能取餐");
        }
        changeOrderState(task.getOrderId(), OrderStatus.WAITING_PICKUP, OrderStatus.DELIVERING,
                rider.getId(), "骑手已取餐，开始配送");
        Order order = requireOrder(task.getOrderId());
        notifyOrderParties(order, rider.getId(), "骑手已取餐，餐品正在配送中", task.getOrderId());
        return deliveryTaskMapper.selectViewById(taskId);
    }

    @Override
    @Transactional
    public DeliveryTaskVO deliver(Long taskId) {
        User rider = requireApprovedRider(false);
        DeliveryTask task = requireOwnedTask(taskId, rider.getId());
        requireTransition(task, DeliveryTaskStatus.DELIVERED);
        if (deliveryTaskMapper.markDelivered(taskId, rider.getId()) != 1) {
            throw new APIException("只有配送中的任务才能确认送达");
        }
        changeOrderState(task.getOrderId(), OrderStatus.DELIVERING, OrderStatus.DELIVERED,
                rider.getId(), "骑手已送达，等待顾客确认");
        Order order = requireOrder(task.getOrderId());
        notifyOrderParties(order, rider.getId(), "餐品已送达，请顾客确认收货", task.getOrderId());
        return deliveryTaskMapper.selectViewById(taskId);
    }

    @Override
    @Transactional
    public DeliveryTaskVO confirmReceipt(Long orderId) {
        User customer = currentUser();
        Order order = requireOrder(orderId);
        if (!Objects.equals(order.getCustomerId(), customer.getId())) {
            throw new APIException("只有下单顾客可以确认收货");
        }
        if ("PICKUP".equalsIgnoreCase(order.getServiceMode())) {
            if (!Objects.equals(order.getOrderState(), OrderStatus.WAITING_PICKUP.getCode())) {
                throw new APIException("商家尚未确认出餐，暂不能确认取餐");
            }
            changeOrderState(orderId, OrderStatus.WAITING_PICKUP, OrderStatus.COMPLETED,
                    customer.getId(), "顾客到店取餐并确认完成");
            awardCustomerPoints(order, customer.getId());
            notifyMerchant(order, "顾客已到店取餐，订单完成", orderId);
            return null;
        }
        DeliveryTask task = deliveryTaskMapper.selectByOrderId(orderId);
        if (task == null || !DeliveryTaskStatus.DELIVERED.name().equals(task.getTaskStatus())) {
            throw new APIException("骑手尚未确认送达");
        }
        if (deliveryTaskMapper.markCompleted(task.getId()) != 1) {
            throw new APIException("配送任务状态已变化，请刷新后重试");
        }
        changeOrderState(orderId, OrderStatus.DELIVERED, OrderStatus.COMPLETED,
                customer.getId(), "顾客确认收货");
        riderMapper.addCompletedStats(task.getRiderUserId(), safe(task.getDistanceKm()), safe(task.getRiderFee()));
        awardCustomerPoints(order, customer.getId());
        notifyUser(task.getRiderUserId(), "订单 #" + orderId + " 已完成，配送收入已计入", orderId);
        notifyMerchant(order, "顾客已确认收货，订单完成", orderId);
        return deliveryTaskMapper.selectViewById(task.getId());
    }

    @Override
    @Transactional
    public DeliveryException reportException(Long taskId, DeliveryExceptionCreateDTO dto) {
        User rider = requireApprovedRider(false);
        DeliveryTask task = requireOwnedTask(taskId, rider.getId());
        DeliveryTaskStatus currentStatus = DeliveryTaskStatus.fromName(task.getTaskStatus());
        requireTransition(task, DeliveryTaskStatus.EXCEPTION);
        if (deliveryTaskMapper.selectOpenExceptionByTask(taskId) != null) {
            throw new APIException("该任务已有待处理异常");
        }

        DeliveryException deliveryException = new DeliveryException();
        deliveryException.setTaskId(taskId);
        deliveryException.setRiderUserId(rider.getId());
        deliveryException.setExceptionType(dto.getExceptionType());
        deliveryException.setDescription(dto.getDescription().trim());
        deliveryException.setPreviousTaskStatus(currentStatus.name());
        deliveryException.setStatus(0);
        deliveryTaskMapper.insertException(deliveryException);
        if (deliveryTaskMapper.markException(taskId, rider.getId()) != 1) {
            throw new APIException("上报异常失败，任务状态已变化");
        }

        Order order = requireOrder(task.getOrderId());
        OrderStatus previousOrderStatus = OrderStatus.fromCode(order.getOrderState());
        changeOrderState(task.getOrderId(), previousOrderStatus, OrderStatus.DELIVERY_EXCEPTION,
                rider.getId(), "骑手上报异常：" + dto.getExceptionType());
        notifyOrderParties(order, rider.getId(), "配送出现异常，管理员正在处理", task.getOrderId());
        broadcastDeliveryUpdate("配送任务 #" + taskId + " 出现异常", task.getOrderId());
        return deliveryException;
    }

    @Override
    public List<DeliveryExceptionVO> listExceptions(Integer status) {
        return deliveryTaskMapper.listExceptions(status);
    }

    @Override
    @Transactional
    public DeliveryTaskVO resolveException(Long exceptionId, DeliveryExceptionResolveDTO dto) {
        User admin = currentUser();
        DeliveryException deliveryException = deliveryTaskMapper.selectExceptionById(exceptionId);
        if (deliveryException == null || deliveryException.getStatus() != 0) {
            throw new APIException("待处理配送异常不存在");
        }
        DeliveryTask task = requireTask(deliveryException.getTaskId());
        if (!DeliveryTaskStatus.EXCEPTION.name().equals(task.getTaskStatus())) {
            throw new APIException("配送任务已不处于异常状态");
        }
        Order order = requireOrder(task.getOrderId());
        OrderStatus targetOrderStatus;

        switch (dto.getAction()) {
            case "RESUME" -> {
                DeliveryTaskStatus target = DeliveryTaskStatus.fromName(deliveryException.getPreviousTaskStatus());
                if (!DeliveryTaskStatus.EXCEPTION.canTransitionTo(target)
                        || deliveryTaskMapper.resumeFromException(task.getId(), target.name()) != 1) {
                    throw new APIException("恢复配送失败");
                }
                targetOrderStatus = orderStatusForTask(target);
            }
            case "REASSIGN" -> {
                if (deliveryTaskMapper.reassignFromException(task.getId()) != 1) {
                    throw new APIException("重新派单失败");
                }
                targetOrderStatus = OrderStatus.WAITING_RIDER_ACCEPT;
            }
            case "CANCEL" -> {
                if (deliveryTaskMapper.cancelFromException(task.getId()) != 1) {
                    throw new APIException("取消配送失败");
                }
                targetOrderStatus = OrderStatus.CANCELLED;
                foodMapper.restoreStockByOrder(task.getOrderId());
                assetMapper.releaseCouponByOrder(task.getOrderId());
                ordersMapper.updatePaymentStatus(task.getOrderId(), "REFUNDED");
                if (Boolean.TRUE.equals(order.getWalletPaid()) || (order.getPointsUsed() != null && order.getPointsUsed() > 0)) {
                    assetService.refundOrderAssets(task.getOrderId(), order.getCustomerId(), order.getPointsUsed(),
                            Boolean.TRUE.equals(order.getWalletPaid()) ? order.getOrderTotal() : BigDecimal.ZERO);
                }
            }
            default -> throw new APIException("不支持的异常处理动作");
        }

        changeOrderState(task.getOrderId(), OrderStatus.DELIVERY_EXCEPTION, targetOrderStatus,
                admin.getId(), "管理员处理配送异常：" + dto.getAction());
        if (deliveryTaskMapper.resolveException(exceptionId, dto.getAction(), dto.getNote(), admin.getId()) != 1) {
            throw new APIException("异常处理记录保存失败");
        }
        String content = switch (dto.getAction()) {
            case "RESUME" -> "配送异常已处理，请继续配送";
            case "REASSIGN" -> "配送任务已重新派单";
            default -> "配送异常已处理，订单已取消";
        };
        notifyOrderParties(order, task.getRiderUserId(), content, task.getOrderId());
        return deliveryTaskMapper.selectViewById(task.getId());
    }

    @Override
    public List<OrderStatusHistory> listOrderHistory(Long orderId) {
        User current = currentUser();
        Order order = requireOrder(orderId);
        DeliveryTask task = deliveryTaskMapper.selectByOrderId(orderId);
        assertOrderVisibleTo(current, order, task);
        return historyMapper.listByOrderId(orderId);
    }

    private User currentUser() {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new APIException("未获取到当前登录用户"));
        User user = userMapper.findByUsernameWithAuthorities(username);
        if (user == null) {
            throw new APIException("当前用户不存在");
        }
        return user;
    }

    private User requireApprovedRider(boolean mustBeOnline) {
        User user = currentUser();
        RiderProfile profile = riderMapper.findByUserId(user.getId());
        if (profile == null || profile.getAuditStatus() != RiderAuditStatus.APPROVED) {
            throw new APIException("骑手身份尚未审核通过");
        }
        if (mustBeOnline && !Boolean.TRUE.equals(profile.getOnline())) {
            throw new APIException("请先上线再查看或接取配送任务");
        }
        return user;
    }

    private DeliveryTask requireTask(Long taskId) {
        DeliveryTask task = deliveryTaskMapper.selectById(taskId);
        if (task == null) {
            throw new APIException("配送任务不存在");
        }
        return task;
    }

    private DeliveryTask requireOwnedTask(Long taskId, Long riderUserId) {
        DeliveryTask task = requireTask(taskId);
        if (!Objects.equals(task.getRiderUserId(), riderUserId)) {
            throw new APIException("不能操作其他骑手的配送任务");
        }
        return task;
    }

    private Order requireOrder(Long orderId) {
        Order order = ordersMapper.getOrderById(orderId);
        if (order == null) {
            throw new APIException("订单不存在");
        }
        return order;
    }

    private void requireBusinessOwner(Long userId, Long businessId) {
        Business business = businessMapper.selectBusinessById(businessId);
        if (business == null || !Objects.equals(business.getUserId(), userId)) {
            throw new APIException("只能操作自己店铺的订单");
        }
    }

    private void requireTransition(DeliveryTask task, DeliveryTaskStatus target) {
        DeliveryTaskStatus source = DeliveryTaskStatus.fromName(task.getTaskStatus());
        if (!source.canTransitionTo(target)) {
            throw new APIException("配送状态不能从“" + source.getLabel() + "”变为“" + target.getLabel() + "”");
        }
    }

    private void changeOrderState(Long orderId,
                                  OrderStatus expected,
                                  OrderStatus target,
                                  Long operatorUserId,
                                  String reason) {
        int changed = ordersMapper.updateOrderStateIfCurrent(orderId, expected.getCode(), target.getCode(), operatorUserId);
        if (changed != 1) {
            throw new APIException("订单状态已变化，请刷新后重试");
        }
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setFromStatus(expected.getCode());
        history.setToStatus(target.getCode());
        history.setOperatorUserId(operatorUserId);
        history.setReason(reason);
        historyMapper.insert(history);
    }

    private void assertTaskVisibleTo(User user, DeliveryTask task) {
        Order order = requireOrder(task.getOrderId());
        assertOrderVisibleTo(user, order, task);
    }

    private void assertOrderVisibleTo(User user, Order order, DeliveryTask task) {
        boolean admin = hasAuthority(user, "ADMIN");
        boolean customer = Objects.equals(order.getCustomerId(), user.getId());
        Business business = businessMapper.selectBusinessById(order.getBusinessId());
        boolean merchant = business != null && Objects.equals(business.getUserId(), user.getId());
        boolean rider = task != null && Objects.equals(task.getRiderUserId(), user.getId());
        if (!admin && !customer && !merchant && !rider) {
            throw new APIException("无权查看该订单的配送信息");
        }
    }

    private boolean hasAuthority(User user, String authority) {
        return user.getAuthorities() != null
                && user.getAuthorities().stream().anyMatch(item -> authority.equals(item.getName()));
    }

    private void hideCustomerPrivacyBeforeAccept(DeliveryTaskVO task) {
        task.setContactName("接单后可见");
        task.setContactTel(null);
        String address = task.getDeliveryAddress();
        if (address != null && address.length() > 8) {
            task.setDeliveryAddress(address.substring(0, 8) + "…（接单后可见）");
        }
    }

    private OrderStatus orderStatusForTask(DeliveryTaskStatus taskStatus) {
        return switch (taskStatus) {
            case ACCEPTED, ARRIVED_STORE -> OrderStatus.WAITING_PICKUP;
            case DELIVERING -> OrderStatus.DELIVERING;
            case DELIVERED -> OrderStatus.DELIVERED;
            case WAITING_RIDER -> OrderStatus.WAITING_RIDER_ACCEPT;
            case CANCELLED -> OrderStatus.CANCELLED;
            case COMPLETED -> OrderStatus.COMPLETED;
            case EXCEPTION -> OrderStatus.DELIVERY_EXCEPTION;
        };
    }

    private BigDecimal estimateDistance(Long orderId) {
        double distance = 1.8 + (orderId % 5) * 0.7;
        return BigDecimal.valueOf(distance).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void notifyOrderParties(Order order, Long riderUserId, String content, Long orderId) {
        notifyUser(order.getCustomerId(), content, orderId);
        notifyMerchant(order, content, orderId);
        if (riderUserId != null) {
            notifyUser(riderUserId, content, orderId);
        }
    }

    private void notifyMerchant(Order order, String content, Long orderId) {
        Business business = businessMapper.selectBusinessById(order.getBusinessId());
        if (business != null) {
            notifyUser(business.getUserId(), content, orderId);
        }
    }

    private void awardCustomerPoints(Order order, Long customerId) {
        assetMapper.ensure(customerId);
        int earnedPoints = order.getOrderTotal() == null ? 0 : order.getOrderTotal().setScale(0, RoundingMode.FLOOR).intValue();
        if (earnedPoints <= 0) return;
        assetMapper.addPoints(customerId, earnedPoints);
        assetMapper.insertLedger(customerId, "POINT_EARN", BigDecimal.ZERO, earnedPoints, "完成订单奖励积分", order.getId());
    }

    private void notifyUser(Long userId, String content, Long orderId) {
        if (userId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setNotificationType(3);
        notification.setNotificationContent(content + "（订单 #" + orderId + "）");
        notification.setAuditResult(0);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        notification.setIsDeleted(0);
        notificationMapper.insert(notification);

        JSONObject message = new JSONObject();
        message.put("type", "delivery_update");
        message.put("orderId", orderId);
        message.put("content", content);
        webSocketServer.sendToClient(userId.toString(), message.toJSONString());
    }

    private void broadcastDeliveryUpdate(String content, Long orderId) {
        JSONObject message = new JSONObject();
        message.put("type", "delivery_update");
        message.put("orderId", orderId);
        message.put("content", content);
        webSocketServer.sendToAllClient(message.toJSONString());
    }
}
