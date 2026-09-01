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

    Long orderSubmit(Long businessId,Long addressId);
}
