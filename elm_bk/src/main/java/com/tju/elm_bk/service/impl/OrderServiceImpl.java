package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.dto.OrderDTO;
import com.tju.elm_bk.constant.OrderStatus;
import com.tju.elm_bk.entity.*;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.*;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.service.OrderService;
import com.tju.elm_bk.service.CurrentUserService;
import com.tju.elm_bk.service.OrderSettlementService;
import com.tju.elm_bk.service.OrderStateTransitionService;
import com.tju.elm_bk.service.OrderSubmissionService;
import com.tju.elm_bk.vo.OrderItemDetailVO;
import com.tju.elm_bk.vo.OrderItemVO;
import com.tju.elm_bk.vo.OrderVO;
import com.tju.elm_bk.websocket.WebSocketServer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrdersMapper ordersMapper;
    private final BusinessMapper businessMapper;
    private final OrderDetailetMapper orderDetailetMapper;
    private final WebSocketServer webSocketServer;
    private final CurrentUserService currentUserService;
    private final OrderStateTransitionService orderStateTransitionService;
    private final OrderSubmissionService orderSubmissionService;
    private final OrderSettlementService orderSettlementService;

    // 兼容旧前端的通用状态接口：只允许“支付”和“取消”。
    // 商家接单、骑手履约和顾客收货必须走配送域专用接口，避免越级改状态。
    public static final List<Integer> orderStatusList = List.of(
            OrderStatus.WAITING_MERCHANT_ACCEPT.getCode(),
            OrderStatus.CANCELLED.getCode()
    );

    @Override
    public List<OrderVO> getCustomerOrderList(Long customerId) {
        User user = currentUserService.requireUser();
        if (!Objects.equals(customerId, user.getId()) && !currentUserService.isAdmin(user)) {
            throw new APIException(ResultCodeEnum.USER_UNMATCHED);
        }
        return ordersMapper.selectOrders(customerId);
    }

    @Override
    public OrderVO getOrderById(Long orderId) {
        User user = currentUserService.requireUser();
        OrderVO orderVO = ordersMapper.selectOrderById(orderId);
        if (orderVO == null) {
            throw new APIException(ResultCodeEnum.ORDER_MISSED);
        }
        Order order = ordersMapper.getOrderById(orderId);
        Business business = businessMapper.selectBusinessById(order.getBusinessId());
        boolean admin = currentUserService.isAdmin(user);
        boolean merchant = business != null && Objects.equals(business.getUserId(), user.getId());
        Long customerId = orderVO.getCustomer() == null ? null : orderVO.getCustomer().getId();
        if(!admin && !Objects.equals(customerId, user.getId()) && !merchant) {
            throw new APIException(ResultCodeEnum.USER_UNMATCHED);
        }
        return orderVO;
    }

    @Override
    @Transactional
    public OrderVO addOrder(OrderDTO orderDTO) {
        if (orderDTO == null || !orderDTO.verify()) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
        String currentUsername = currentUserService.requireUser().getUsername();
        if (!Objects.equals(currentUsername, orderDTO.getCustomer().getUsername())) {
            throw new APIException(ResultCodeEnum.USER_UNMATCHED);
        }
        // 兼容老师测试用的旧请求格式，但所有身份、地址、商品价格、优惠和配送费
        // 仍统一走正式下单服务；customer 只做一致性校验，orderTotal 等字段不作为事实来源。
        Long orderId = orderSubmit(orderDTO.getBusiness().getId(), orderDTO.getDeliveryAddress().getId(),
                null, "delivery", null);
        return ordersMapper.selectOrderById(orderId);
    }





    @Override
    public List<OrderItemDetailVO> getOrderItemListByBusiness(Long businessId, Integer orderState) {
        User currentUser = currentUser();
        if (businessId == null) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
        Business business = businessMapper.selectBusinessById(businessId);
        if (business == null) {
            throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        }
        if (!isAdmin(currentUser) && !Objects.equals(business.getUserId(), currentUser.getId())) {
            throw new APIException(ResultCodeEnum.USER_DENIED);
        }
        List<OrderItemDetailVO> ret = ordersMapper.selectOrderDetailetItem(businessId, orderState);
        for (OrderItemDetailVO orderItemDetailVO : ret) {
            orderItemDetailVO.setFoodList(orderDetailetMapper.selectOrderDetailList(orderItemDetailVO.getId()));
        }
        return ret;
    }

    @Override
    public List<OrderItemVO> getOrderItemListByUser(Integer orderState) {
        Long userId = currentUserService.requireUserId();
        return ordersMapper.selectOrderItemsList(null, orderState, userId);
    }

    @Override
    public OrderItemDetailVO getOrderItemDetail(Long orderItemId) {
        OrderItemDetailVO ret = ordersMapper.selectOrderItemById(orderItemId);
        if (ret == null) {
            throw new APIException(ResultCodeEnum.ORDER_MISSED);
        }
        User currentUser = currentUser();
        boolean customer = Objects.equals(ret.getCustomerId(), currentUser.getId());
        Business business = ret.getBusinessId() == null ? null : businessMapper.selectBusinessById(ret.getBusinessId());
        boolean merchant = business != null && Objects.equals(business.getUserId(), currentUser.getId());
        if (!isAdmin(currentUser) && !customer && !merchant) {
            throw new APIException(ResultCodeEnum.USER_DENIED);
        }
        ret.setFoodList(orderDetailetMapper.selectOrderDetailList(orderItemId));
        return ret;
    }

    private User currentUser() {
        return currentUserService.requireUser();
    }

    private boolean isAdmin(User user) {
        return currentUserService.isAdmin(user);
    }

    @Override
    @Transactional
    public Long setOrderState(Long orderId, Integer orderState) {
        return setOrderState(orderId, orderState, "simulated", 0);
    }

    @Override
    @Transactional
    public Long setOrderState(Long orderId, Integer orderState, String paymentMethod, Integer pointsToUse) {
        return setOrderState(orderId, orderState, paymentMethod, pointsToUse, null);
    }

    @Override
    @Transactional
    public Long setOrderState(Long orderId, Integer orderState, String paymentMethod, Integer pointsToUse, Long couponId) {
        if (!orderStatusList.contains(orderState)) {
            throw new APIException(ResultCodeEnum.ORDER_STATUS_UNMATCHED);
        }
        Order order = ordersMapper.getOrderById(orderId);
        Long userId = currentUserService.requireUserId();
        if (null == order) {
            throw new APIException(ResultCodeEnum.ORDER_MISSED);
        }

        Business business = businessMapper.selectBusinessById(order.getBusinessId());

        String reason;
        if (orderState.equals(OrderStatus.WAITING_MERCHANT_ACCEPT.getCode())) {
            if (!Objects.equals(order.getOrderState(), OrderStatus.WAITING_PAYMENT.getCode())
                    || !Objects.equals(userId, order.getCustomerId())) {
                throw new APIException(ResultCodeEnum.ORDER_PAY_FAILED);
            }
            reason = "顾客完成支付";
        } else {
            if (!Objects.equals(order.getOrderState(), OrderStatus.WAITING_PAYMENT.getCode())
                    && !Objects.equals(order.getOrderState(), OrderStatus.WAITING_MERCHANT_ACCEPT.getCode())) {
                throw new APIException(ResultCodeEnum.ORDER_CANCEL_DENY);
            }
            if (business == null || (!Objects.equals(business.getUserId(),userId) && !Objects.equals(order.getCustomerId(),userId))) {
                throw new APIException(ResultCodeEnum.ORDER_CANCEL_FAILED);
            }
            reason = Objects.equals(userId, order.getCustomerId()) ? "顾客取消订单" : "商家取消订单";
        }
        orderStateTransitionService.transition(orderId, OrderStatus.fromCode(order.getOrderState()),
                OrderStatus.fromCode(orderState), userId, reason);
        if (orderState.equals(OrderStatus.WAITING_MERCHANT_ACCEPT.getCode())) {
            orderSettlementService.settlePayment(order, userId, paymentMethod, pointsToUse, couponId);
        }
        if (orderState.equals(OrderStatus.CANCELLED.getCode())) {
            boolean paid = Objects.equals(order.getOrderState(), OrderStatus.WAITING_MERCHANT_ACCEPT.getCode());
            orderSettlementService.cancelOrder(order, paid);
        }
        // 订单状态更新后，推送消息给相关用户
        Order order1 = ordersMapper.getOrderById(orderId);
        // 1. 推送给商家（如果订单关联了商家）
        if (order1.getBusinessId() != null) {
            Business business1 = businessMapper.selectBusinessById(order1.getBusinessId());
            if (business1 != null && business1.getUserId() != null) {
                webSocketServer.sendToClient(business1.getUserId().toString(),
                        "{\"type\": \"order_update\", \"orderId\": " + orderId + "}");
            }
        }
        // 2. 推送给顾客（订单的customerId）
        if (order1.getCustomerId() != null) {
            webSocketServer.sendToClient(order1.getCustomerId().toString(),
                    "{\"type\": \"order_update\", \"orderId\": " + orderId + "}");
        }

        return order.getId();
    }

    @Override
    @Transactional
    public Long orderSubmit(Long businessId, Long addressId, String idempotencyKey) {
        return orderSubmit(businessId, addressId, idempotencyKey, "delivery");
    }

    @Override
    @Transactional
    public Long orderSubmit(Long businessId, Long addressId, String idempotencyKey, String serviceMode) {
        return orderSubmit(businessId, addressId, idempotencyKey, serviceMode, null);
    }

    @Override
    @Transactional
    public Long orderSubmit(Long businessId, Long addressId, String idempotencyKey, String serviceMode,
                            List<Long> selectedFoodIds) {
        return orderSubmissionService.submit(businessId, addressId, idempotencyKey, serviceMode, selectedFoodIds);
    }

}
