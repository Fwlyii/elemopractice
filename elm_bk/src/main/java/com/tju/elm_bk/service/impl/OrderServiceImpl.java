package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.dto.OrderDTO;
import com.tju.elm_bk.constant.OrderStatus;
import com.tju.elm_bk.entity.*;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.*;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.service.OrderService;
import com.tju.elm_bk.utils.SecurityUtils;
import com.tju.elm_bk.vo.CartItemVO;
import com.tju.elm_bk.vo.OrderItemDetailVO;
import com.tju.elm_bk.vo.OrderItemVO;
import com.tju.elm_bk.vo.OrderVO;
import com.tju.elm_bk.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BusinessMapper businessMapper;
    @Autowired
    private FoodMapper foodMapper;
    @Autowired
    private OrderDetailetMapper orderDetailetMapper;
    @Autowired
    private DeliveryAddressMapper deliveryAddressMapper;
    @Autowired
    private WebSocketServer webSocketServer;
    @Autowired
    private OrderStatusHistoryMapper orderStatusHistoryMapper;

    // 兼容旧前端的通用状态接口：只允许“支付”和“取消”。
    // 商家接单、骑手履约和顾客收货必须走配送域专用接口，避免越级改状态。
    public static final List<Integer> orderStatusList = List.of(
            OrderStatus.WAITING_MERCHANT_ACCEPT.getCode(),
            OrderStatus.CANCELLED.getCode()
    );

    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private AssetMapper assetMapper;
    @Autowired
    private com.tju.elm_bk.service.AssetService assetService;

    @Override
    public List<OrderVO> getCustomerOrderList(Long customerId) {
        User user = userMapper.findByUsernameWithAuthorities(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));
        if (!Objects.equals(customerId, user.getId()) &&
                user.getAuthorities().stream().noneMatch(auth -> "ADMIN".equals(auth.getName()))) {
            throw new APIException(ResultCodeEnum.USER_UNMATCHED);
        }
        return ordersMapper.selectOrders(customerId);
    }

    @Override
    public OrderVO getOrderById(Long orderId) {
        User user = userMapper.findByUsernameWithAuthorities(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));
        OrderVO orderVO = ordersMapper.selectOrderById(orderId);
        if (orderVO == null) {
            throw new APIException(ResultCodeEnum.ORDER_MISSED);
        }
        Order order = ordersMapper.getOrderById(orderId);
        Business business = businessMapper.selectBusinessById(order.getBusinessId());
        boolean admin = user.getAuthorities().stream().anyMatch(auth -> "ADMIN".equals(auth.getName()));
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
        if (!orderDTO.verify()) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
        Business business = businessMapper.selectBusinessById(orderDTO.getBusiness().getId());
        if (business == null) {
            throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        }
        ensureBusinessOrderable(business);
        DeliveryAddress deliveryAddress = deliveryAddressMapper.getDeliveryAddressById(orderDTO.getDeliveryAddress().getId());
        if (deliveryAddress == null || Boolean.TRUE.equals(deliveryAddress.getIsDeleted())) {
            throw new APIException(ResultCodeEnum.ADDRESS_MISSED);
        }

        // 设置订单信息
        User user = userMapper.findByUsernameWithAuthorities(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));
        if (!Objects.equals(userMapper.getUserIdByUsername(orderDTO.getCustomer().getUsername()), user.getId())) {
            throw new APIException(ResultCodeEnum.USER_UNMATCHED);
        }
        if (!Objects.equals(deliveryAddress.getUserId(), user.getId())) {
            throw new APIException(ResultCodeEnum.ADDRESS_PERMISSION_DENIED);
        }
        List<CartItemVO> cartItemsInBusiness = cartMapper.selectCartItems(user.getId(), business.getId());
        if (cartItemsInBusiness.isEmpty()) {
            throw new APIException(ResultCodeEnum.CART_EMPTY);
        }
        validatePurchaseLimits(cartItemsInBusiness);
        reserveStock(cartItemsInBusiness);
        Order order = new Order();
        order.setBusinessId(business.getId());
        order.setOrderDate(LocalDateTime.now());
        order.setCustomerId(user.getId());
        order.setAddressId(deliveryAddress.getId());
        copyAddressSnapshot(order, deliveryAddress);
        order.setDeliveryPrice(business.getDeliveryPrice());

        order.setOrderState(0);
        order.setCreator(user.getId());
        order.setUpdater(user.getId());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setIsDeleted(false);

        // 计算总价
        double totalPrice = 0.0;
        for (CartItemVO cartItemVO : cartItemsInBusiness) {
            totalPrice += (cartItemVO.getFoodPrice() * cartItemVO.getQuantity());
        }
        order.setOrderTotal(BigDecimal.valueOf(totalPrice));
        validateStartPrice(business, order.getOrderTotal());

        // 插入订单数据到数据库
        ordersMapper.insertOrder(order);
        recordStatus(order.getId(), null, OrderStatus.WAITING_PAYMENT.getCode(), user.getId(), "顾客创建订单");

        // 插入订单详情
        for (CartItemVO cartItemVO : cartItemsInBusiness) {

            OrderDetailet orderDetailet = new OrderDetailet();

            orderDetailet.setOrderId(order.getId());
            orderDetailet.setQuantity(cartItemVO.getQuantity());
            orderDetailet.setFoodId(cartItemVO.getFoodId());

            orderDetailet.setCreator(user.getId());
            orderDetailet.setUpdater(user.getId());
            orderDetailet.setCreateTime(LocalDateTime.now());
            orderDetailet.setUpdateTime(LocalDateTime.now());
            orderDetailet.setIsDeleted(false);

            orderDetailetMapper.saveOrderDetail(orderDetailet);
        }

        // 清空该用户在当前商家的购物车
        cartMapper.clearCart(user.getId(),orderDTO.getBusiness().getId());

        return ordersMapper.selectOrderById(order.getId());
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
        Long userId = userMapper.getUserIdByUsername(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));
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
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED));
        User user = userMapper.findByUsernameWithAuthorities(username);
        if (user == null) throw new APIException(ResultCodeEnum.USER_MISSED);
        return user;
    }

    private boolean isAdmin(User user) {
        return user.getAuthorities() != null && user.getAuthorities().stream()
                .anyMatch(auth -> "ADMIN".equals(auth.getName()));
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
        Long userId = userMapper.getUserIdByUsername(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));
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
        if (ordersMapper.updateOrderStateIfCurrent(orderId, order.getOrderState(), orderState, userId) != 1) {
            throw new APIException("订单状态已变化，请刷新后重试");
        }
        if (orderState.equals(OrderStatus.WAITING_MERCHANT_ACCEPT.getCode())) {
            settlePayment(order, userId, paymentMethod, pointsToUse, couponId);
        }
        if (orderState.equals(OrderStatus.CANCELLED.getCode())) {
            foodMapper.restoreStockByOrder(orderId);
            assetMapper.releaseCouponByOrder(orderId);
            ordersMapper.updatePaymentStatus(orderId,
                    Objects.equals(order.getOrderState(), OrderStatus.WAITING_MERCHANT_ACCEPT.getCode()) ? "REFUNDED" : "CANCELLED");
            if (Boolean.TRUE.equals(order.getWalletPaid()) || (order.getPointsUsed() != null && order.getPointsUsed() > 0)) {
                assetService.refundOrderAssets(orderId, order.getCustomerId(), order.getPointsUsed(),
                        Boolean.TRUE.equals(order.getWalletPaid()) ? order.getOrderTotal() : BigDecimal.ZERO);
            }
        }
        recordStatus(orderId, order.getOrderState(), orderState, userId, reason);
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

    /**
     * 支付在状态从待支付切换为待商家接单后结算，避免并发重复扣款。
     * 模拟支付不扣钱包；钱包支付和积分抵扣都使用条件更新保证余额不会变负。
     */
    private void settlePayment(Order order, Long userId, String requestedMethod, Integer requestedPoints, Long couponId) {
        String method = requestedMethod == null ? "simulated" : requestedMethod.trim().toLowerCase();
        boolean wallet = "wallet".equals(method);
        if (!wallet && !"simulated".equals(method) && !"alipay".equals(method) && !"wechat".equals(method)) {
            throw new APIException("不支持的支付方式");
        }
        int points = requestedPoints == null ? 0 : requestedPoints;
        if (points < 0 || points % 100 != 0) {
            throw new APIException("积分抵扣必须为100的整数倍");
        }
        BigDecimal beforePoints = order.getOrderTotal() == null ? BigDecimal.ZERO : order.getOrderTotal();
        UserCoupon selectedCoupon = null;
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (couponId != null) {
            selectedCoupon = assetMapper.findCouponForUser(couponId, userId);
            if (selectedCoupon == null) {
                throw new APIException("红包不可用或已过期，请刷新结算页");
            }
            BigDecimal couponBase = beforePoints.subtract(order.getDeliveryPrice() == null ? BigDecimal.ZERO : order.getDeliveryPrice()).max(BigDecimal.ZERO);
            if (selectedCoupon.getMinOrderAmount() != null && couponBase.compareTo(selectedCoupon.getMinOrderAmount()) < 0) {
                throw new APIException("当前订单未达到该红包使用门槛");
            }
            couponDiscount = selectedCoupon.getDiscountAmount() == null ? BigDecimal.ZERO : selectedCoupon.getDiscountAmount().max(BigDecimal.ZERO).min(couponBase);
        }
        BigDecimal beforePointDiscount = beforePoints.subtract(couponDiscount).max(BigDecimal.ZERO);
        int maxPoints = beforePointDiscount.multiply(new BigDecimal("0.20")).multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.FLOOR).intValue();
        if (points > maxPoints) {
            throw new APIException("本单积分最多抵扣应付金额的20%");
        }
        assetMapper.ensure(userId);
        if (points > 0 && assetMapper.subtractPointsIfEnough(userId, points) != 1) {
            throw new APIException("积分余额不足");
        }
        BigDecimal pointDiscount = BigDecimal.valueOf(points).divide(new BigDecimal("100"), 2, RoundingMode.DOWN);
        BigDecimal payable = beforePointDiscount.subtract(pointDiscount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        if (points > 0) {
            assetMapper.insertLedger(userId, "POINT_REDEEM", BigDecimal.ZERO, -points, "订单支付积分抵扣", order.getId());
        }
        if (wallet && assetMapper.subtractBalanceIfEnough(userId, payable) != 1) {
            throw new APIException("钱包余额不足，请选择其他支付方式或先充值");
        }
        if (wallet && payable.compareTo(BigDecimal.ZERO) > 0) {
            assetMapper.insertLedger(userId, "WALLET_PAY", payable.negate(), 0, "订单钱包支付", order.getId());
        }
        if (selectedCoupon != null && assetMapper.useCoupon(selectedCoupon.getId(), order.getId()) != 1) {
            throw new APIException("红包已被使用，请刷新结算页");
        }
        String storedMethod = wallet ? "WALLET" : "SIMULATED";
        if (ordersMapper.updateOrderPayment(order.getId(), payable, storedMethod, points, wallet, userId) != 1) {
            throw new APIException("支付记录保存失败，请重试");
        }
        order.setOrderTotal(payable);
        order.setPointsUsed(points);
        order.setWalletPaid(wallet);
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
        String normalizedKey = idempotencyKey == null ? null : idempotencyKey.trim();
        if (normalizedKey != null && normalizedKey.isEmpty()) normalizedKey = null;
        if (normalizedKey != null && normalizedKey.length() > 64) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
        // 商家是否存在
        Business business = businessMapper.selectBusinessById(businessId);
        if (null == business) {
            throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        }
        ensureBusinessOrderable(business);

        String normalizedServiceMode = normalizeServiceMode(serviceMode);
        if ("PICKUP".equals(normalizedServiceMode) && !Boolean.TRUE.equals(business.getDineInAvailable())) {
            throw new APIException("该商家暂不支持到店自取，请选择外送");
        }

        // 设置订单信息
        Long userId = userMapper.getUserIdByUsername(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));
        if (normalizedKey != null) {
            Long existingOrderId = ordersMapper.findIdByIdempotencyKey(userId, normalizedKey);
            if (existingOrderId != null) return existingOrderId;
        }
        DeliveryAddress deliveryAddress = null;
        if ("DELIVERY".equals(normalizedServiceMode)) {
            if (addressId == null) {
                throw new APIException("外送订单请选择收货地址");
            }
            deliveryAddress = deliveryAddressMapper.getDeliveryAddressById(addressId);
            if (deliveryAddress == null || Boolean.TRUE.equals(deliveryAddress.getIsDeleted())
                    || !Objects.equals(deliveryAddress.getUserId(), userId)) {
                throw new APIException(ResultCodeEnum.ADDRESS_PERMISSION_DENIED);
            }
        } else if (addressId != null) {
            // 自取不需要地址；若旧客户端仍传地址，只校验归属后忽略，避免把他人地址写进订单。
            deliveryAddress = deliveryAddressMapper.getDeliveryAddressById(addressId);
            if (deliveryAddress == null || Boolean.TRUE.equals(deliveryAddress.getIsDeleted())
                    || !Objects.equals(deliveryAddress.getUserId(), userId)) {
                throw new APIException(ResultCodeEnum.ADDRESS_PERMISSION_DENIED);
            }
        }
        List<CartItemVO> cartItemsInBusiness = cartMapper.selectCartItems(userId,businessId);
        if (selectedFoodIds != null && !selectedFoodIds.isEmpty()) {
            java.util.Set<Long> selected = new java.util.HashSet<>(selectedFoodIds);
            cartItemsInBusiness = cartItemsInBusiness == null ? java.util.Collections.emptyList() : cartItemsInBusiness.stream()
                    .filter(item -> item.getFoodId() != null && selected.contains(item.getFoodId()))
                    .collect(java.util.stream.Collectors.toList());
        }
        if (cartItemsInBusiness == null || cartItemsInBusiness.isEmpty()) {
            throw new APIException(ResultCodeEnum.CART_EMPTY);
        }
        validatePurchaseLimits(cartItemsInBusiness);
        Order order = new Order();
        order.setBusinessId(businessId);
        order.setOrderDate(LocalDateTime.now());
        order.setCustomerId(userId);
        order.setIdempotencyKey(normalizedKey);
        order.setAddressId("DELIVERY".equals(normalizedServiceMode) ? addressId : null);
        copyAddressSnapshot(order, deliveryAddress);

        order.setOrderState(0);
        order.setCreator(userId);
        order.setUpdater(userId);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setIsDeleted(false);

        // 计算总价
        double totalPrice = 0.0;
        for (CartItemVO cartItemVO : cartItemsInBusiness) {
            totalPrice += (cartItemVO.getFoodPrice() * cartItemVO.getQuantity());
        }

        if (totalPrice == 0.0) {
            throw new APIException(ResultCodeEnum.ORDER_SUBMIT_FAILED);
        }
        // 在创建订单事务中原子预占库存；任一商品不足会回滚整笔订单。
        reserveStock(cartItemsInBusiness);
        BigDecimal subtotal = BigDecimal.valueOf(totalPrice).setScale(2, RoundingMode.HALF_UP);
        validateStartPrice(business, subtotal);
        UserAsset assets = assetMapper.findByUserId(userId);
        BigDecimal merchantPromotion = BigDecimal.ZERO;
        if (business.getPromotionThreshold() != null && business.getPromotionDiscount() != null
                && subtotal.compareTo(business.getPromotionThreshold()) >= 0
                && business.getPromotionDiscount().compareTo(BigDecimal.ZERO) > 0
                && business.getPromotionDiscount().compareTo(subtotal) < 0) {
            merchantPromotion = business.getPromotionDiscount();
        }
        BigDecimal discountedSubtotal = subtotal.subtract(merchantPromotion).max(BigDecimal.ZERO);
        if (assets != null && assets.getMembershipExpire() != null && assets.getMembershipExpire().isAfter(LocalDateTime.now())) {
            // 会员折扣与商家满减叠加，不能覆盖已经计算出的商家优惠。
            discountedSubtotal = discountedSubtotal.multiply(new BigDecimal("0.95"));
        }
        BigDecimal orderDeliveryPrice = "PICKUP".equals(normalizedServiceMode) ? BigDecimal.ZERO : (business.getDeliveryPrice() == null ? BigDecimal.ZERO : business.getDeliveryPrice());
        BigDecimal price = discountedSubtotal.add(orderDeliveryPrice).max(BigDecimal.ZERO);
        // 浮点数精度,保留两位小数
        order.setOrderTotal(price.setScale(2, RoundingMode.HALF_UP));
        // 订单保存当前商家配送费,避免商家修改导致的不一致
        order.setDeliveryPrice(orderDeliveryPrice);
        order.setServiceMode(normalizedServiceMode);

        // 插入订单数据到数据库
        ordersMapper.insertOrderPlus(order);
        recordStatus(order.getId(), null, OrderStatus.WAITING_PAYMENT.getCode(), userId, "顾客创建订单");

        // 插入订单详情
        for (CartItemVO cartItemVO : cartItemsInBusiness) {

            OrderDetailet orderDetailet = new OrderDetailet();

            orderDetailet.setOrderId(order.getId());
            orderDetailet.setQuantity(cartItemVO.getQuantity());
            orderDetailet.setFoodId(cartItemVO.getFoodId());
            // 商品价格保存到detail里,避免商家修改导致用户原有订单数据不一致
            orderDetailet.setFoodPrice(BigDecimal.valueOf(cartItemVO.getFoodPrice()));

            orderDetailet.setCreator(userId);
            orderDetailet.setUpdater(userId);
            orderDetailet.setCreateTime(LocalDateTime.now());
            orderDetailet.setUpdateTime(LocalDateTime.now());
            orderDetailet.setIsDeleted(false);

            orderDetailetMapper.saveOrderDetailPlus(orderDetailet);
        }

        // 只清空本次下单的商品；未选中的商品继续保留在购物车。
        if (selectedFoodIds != null && !selectedFoodIds.isEmpty()) {
            cartMapper.clearCartItems(userId, businessId, new java.util.ArrayList<>(new java.util.HashSet<>(selectedFoodIds)));
        } else {
            cartMapper.clearCart(userId,businessId);
        }

        return order.getId();
    }

    private void recordStatus(Long orderId, Integer fromStatus, Integer toStatus, Long operatorUserId, String reason) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setOperatorUserId(operatorUserId);
        history.setReason(reason);
        orderStatusHistoryMapper.insert(history);
    }

    private void reserveStock(List<CartItemVO> items) {
        for (CartItemVO item : items) {
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            if (quantity <= 0 || foodMapper.decrementStock(item.getFoodId(), quantity) != 1) {
                throw new APIException("商品“" + (item.getFoodName() == null ? "" : item.getFoodName()) + "”库存不足或已下架");
            }
        }
    }

    private void validatePurchaseLimits(List<CartItemVO> items) {
        java.util.Map<Long, Integer> quantities = new java.util.HashMap<>();
        for (CartItemVO item : items) {
            if (item.getFoodId() == null || item.getQuantity() == null) continue;
            quantities.merge(item.getFoodId(), item.getQuantity(), Integer::sum);
        }
        for (CartItemVO item : items) {
            Integer limit = item.getPurchaseLimit();
            Integer total = item.getFoodId() == null ? null : quantities.get(item.getFoodId());
            if (limit != null && total != null && total > limit) {
                throw new APIException("商品“" + (item.getFoodName() == null ? "" : item.getFoodName()) + "”单笔限购" + limit + "份");
            }
        }
    }

    private void copyAddressSnapshot(Order order, DeliveryAddress address) {
        if (address == null) return;
        order.setAddressSnapshot(address.getAddress());
        order.setContactNameSnapshot(address.getContactName());
        order.setContactSexSnapshot(address.getContactSex());
        order.setContactTelSnapshot(address.getContactTel());
    }

    private String normalizeServiceMode(String serviceMode) {
        if (serviceMode == null || serviceMode.isBlank() || "delivery".equalsIgnoreCase(serviceMode)) {
            return "DELIVERY";
        }
        if ("pickup".equalsIgnoreCase(serviceMode)) {
            return "PICKUP";
        }
        throw new APIException("不支持的履约方式，请选择外送或自取");
    }

    private void ensureBusinessOrderable(Business business) {
        if (business.getStatus() != null && business.getStatus() != 1) {
            throw new APIException("商家当前未营业或尚未通过审核");
        }
        if (business.getDeliveryPrice() != null && business.getDeliveryPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new APIException("商家配送费配置异常");
        }
    }

    private void validateStartPrice(Business business, BigDecimal subtotal) {
        BigDecimal startPrice = business.getStartPrice() == null ? BigDecimal.ZERO : business.getStartPrice();
        if (startPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new APIException("商家起送价配置异常");
        }
        if (subtotal.compareTo(startPrice) < 0) {
            throw new APIException("订单未达到商家起送价 ¥" + startPrice.setScale(2, RoundingMode.HALF_UP));
        }
    }
}
