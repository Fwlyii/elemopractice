package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.dto.OrderDTO;
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

    // 订单状态(0-待支付,1-待接单,2-已接单,3-已完成,4-已取消
    public static final List<Integer> orderStatusList;

    static {
        orderStatusList = List.of(0,1,2,3,4);
    }

    @Autowired
    private CartMapper cartMapper;

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
        Business business = businessMapper.selectBusinessById(orderId);
        if(!Objects.equals(orderVO.getCustomer().getId(), user.getId()) && !Objects.equals(orderVO.getBusiness().getId(), business.getId())) {
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
        DeliveryAddress deliveryAddress = deliveryAddressMapper.getDeliveryAddressById(orderDTO.getDeliveryAddress().getId());
        if (deliveryAddress == null) {
            throw new APIException(ResultCodeEnum.ADDRESS_MISSED);
        }

        // 设置订单信息
        User user = userMapper.findByUsernameWithAuthorities(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));
        if (!Objects.equals(userMapper.getUserIdByUsername(orderDTO.getCustomer().getUsername()), user.getId())) {
            throw new APIException(ResultCodeEnum.USER_UNMATCHED);
        }
        List<CartItemVO> cartItemsInBusiness = cartMapper.selectCartItems(user.getId(), business.getId());
        if (!cartItemsInBusiness.isEmpty()) {
            throw new APIException(ResultCodeEnum.CART_EMPTY);
        }
        Order order = new Order();
        order.setBusinessId(business.getId());
        order.setOrderDate(LocalDateTime.now());
        order.setCustomerId(user.getId());
        order.setAddressId(deliveryAddress.getId());

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

        // 插入订单数据到数据库
        ordersMapper.insertOrder(order);

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
        ret.setFoodList(orderDetailetMapper.selectOrderDetailList(orderItemId));
        return ret;
    }

    @Override
    public Long setOrderState(Long orderId, Integer orderState) {
        if (!orderStatusList.contains(orderState)) {
            throw new APIException(ResultCodeEnum.ORDER_STATUS_UNMATCHED);
        }
        Order order = ordersMapper.getOrderById(orderId);
        Long userId = userMapper.getUserIdByUsername(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));
        if (null == order) {
            throw new APIException(ResultCodeEnum.ORDER_MISSED);
        }

        Business business = businessMapper.selectBusinessById(order.getBusinessId());

        // 订单状态(0-待支付,1-待接单,2-已接单,3-已完成,4-已取消)
        if (orderState == 1) {
            if (order.getOrderState() != 0 || !Objects.equals(userId, order.getCustomerId())) {
                throw new APIException(ResultCodeEnum.ORDER_PAY_FAILED);
            }
        }
        if (orderState == 2) {
            if (order.getOrderState() != 1 || !Objects.equals(business.getUserId(),userId)) {
                throw new APIException(ResultCodeEnum.ORDER_ACCEPT_FAILED);
            }
        }
        if (orderState == 3) {
            if (order.getOrderState() != 2 || (!Objects.equals(business.getUserId(),userId) && !Objects.equals(order.getCustomerId(),userId))) {
                throw new APIException(ResultCodeEnum.ORDER_ACCEPT_FAILED);
            }
        }
        if (orderState == 4) {
            if (order.getOrderState() != 0 && order.getOrderState() != 1) {
                throw new APIException(ResultCodeEnum.ORDER_CANCEL_DENY);
            }
            if ((!Objects.equals(business.getUserId(),userId) && !Objects.equals(order.getCustomerId(),userId))) {
                throw new APIException(ResultCodeEnum.ORDER_CANCEL_FAILED);
            }
        }
        ordersMapper.setOrderState(orderId, orderState);
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
    public Long orderSubmit(Long businessId,Long addressId) {
        // 商家是否存在
        Business business = businessMapper.selectBusinessById(businessId);
        if (null == business) {
            throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        }

        // 设置订单信息
        Long userId = userMapper.getUserIdByUsername(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));
        List<CartItemVO> cartItemsInBusiness = cartMapper.selectCartItems(userId,businessId);
        Order order = new Order();
        order.setBusinessId(businessId);
        order.setOrderDate(LocalDateTime.now());
        order.setCustomerId(userId);
        order.setAddressId(addressId);

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
        totalPrice += business.getDeliveryPrice().doubleValue();

        // 浮点数精度,保留两位小数
        BigDecimal price = BigDecimal.valueOf(totalPrice);
        order.setOrderTotal(price.setScale(2, RoundingMode.HALF_UP));
        // 订单保存当前商家配送费,避免商家修改导致的不一致
        order.setDeliveryPrice(business.getDeliveryPrice());

        // 插入订单数据到数据库
        ordersMapper.insertOrderPlus(order);

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

        // 清空该用户在当前商家的购物车
        cartMapper.clearCart(userId,businessId);

        return order.getId();
    }
}
