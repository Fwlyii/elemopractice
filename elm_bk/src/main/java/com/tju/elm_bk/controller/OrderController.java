package com.tju.elm_bk.controller;

import com.tju.elm_bk.dto.OrderDTO;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.service.OrderService;
import com.tju.elm_bk.vo.OrderItemDetailVO;
import com.tju.elm_bk.vo.OrderItemVO;
import com.tju.elm_bk.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name="管理订单")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @GetMapping
    @Operation(summary = "获取用户订单列表",description = "老师测试用")
    public HttpResult<List<OrderVO>> listOrdersByUserId (Long userId) {
        return HttpResult.success(orderService.getCustomerOrderList(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据id获取用户订单",description = "老师测试用")
    public HttpResult<OrderVO> getOrderById(@PathVariable Long id) {
        return HttpResult.success(orderService.getOrderById(id));
    }

    @PostMapping
    @Operation(summary = "新增订单",description = "老师测试用")
    public HttpResult<OrderVO> addOrders(@RequestBody OrderDTO orderDTO) {
        return HttpResult.success(orderService.addOrder(orderDTO));
    }




    @GetMapping("/list/business")
    @Operation(summary = "根据商家和状态获取订单列表")
    public HttpResult<List<OrderItemDetailVO>> listOrdersByBusiness(@RequestParam(required = false) Long businessId, @RequestParam(required = false) Integer orderState) {
        return HttpResult.success(orderService.getOrderItemListByBusiness(businessId,orderState));
    }

    @GetMapping("/list/user")
    @Operation(summary = "获取用户自己的相应状态的订单列表")
    public HttpResult<List<OrderItemVO>> listOrdersByUser(@RequestParam(required = false) Integer orderState) {
        return HttpResult.success(orderService.getOrderItemListByUser(orderState));
    }

    @GetMapping("/detail")
    @Operation(summary = "获取订单详情")
    public HttpResult<OrderItemDetailVO> listOrdersByUser(@RequestParam Long orderId) {
        return HttpResult.success(orderService.getOrderItemDetail(orderId));
    }

    @PutMapping("/status")
    @Operation(summary = "设置订单状态",description = "订单状态(0-待支付,1-待接单,2-已接单,3-已完成,4-已取消)")
    public HttpResult<Long> setOrderStatus(@RequestParam Integer orderState,@RequestParam Long orderId) {
        return HttpResult.success(orderService.setOrderState(orderId,orderState));
    }

    @GetMapping("/submit")
    @Operation(summary = "(前端调这个)下单")
    public HttpResult<Long> orderSubmit(@RequestParam Long businessId,@RequestParam Long addressId) {
        return HttpResult.success(orderService.orderSubmit(businessId,addressId));
    }



}
