package com.tju.elm_bk.service;

import com.tju.elm_bk.dto.OrderDTO;
import com.tju.elm_bk.vo.OrderItemDetailVO;
import com.tju.elm_bk.vo.OrderItemVO;
import com.tju.elm_bk.vo.OrderVO;

import java.util.List;

public interface OrderService {

    List<OrderVO> getCustomerOrderList(Long customerId);

    OrderVO getOrderById(Long orderId);

    OrderVO addOrder(OrderDTO orderDTO);




    List<OrderItemDetailVO> getOrderItemListByBusiness(Long businessId, Integer orderState);

    List<OrderItemVO> getOrderItemListByUser(Integer orderState);

    OrderItemDetailVO getOrderItemDetail(Long orderItemId);

    Long setOrderState(Long orderId, Integer orderState);

    Long setOrderState(Long orderId, Integer orderState, String paymentMethod, Integer pointsToUse);

    Long setOrderState(Long orderId, Integer orderState, String paymentMethod, Integer pointsToUse, Long couponId);

    default Long orderSubmit(Long businessId, Long addressId) {
        return orderSubmit(businessId, addressId, null, "delivery");
    }

    Long orderSubmit(Long businessId, Long addressId, String idempotencyKey);

    default Long orderSubmit(Long businessId, Long addressId, String idempotencyKey, String serviceMode) {
        return orderSubmit(businessId, addressId, idempotencyKey);
    }

    /** 提交购物车中选中的商品；foodIds 为空时兼容旧行为。 */
    default Long orderSubmit(Long businessId, Long addressId, String idempotencyKey,
                             String serviceMode, List<Long> foodIds) {
        return orderSubmit(businessId, addressId, idempotencyKey, serviceMode);
    }
}
