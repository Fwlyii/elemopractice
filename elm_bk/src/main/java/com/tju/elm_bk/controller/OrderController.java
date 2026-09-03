package com.tju.elm_bk.controller;

import com.tju.elm_bk.dto.OrderDTO;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.service.OrderService;
import com.tju.elm_bk.vo.OrderItemDetailVO;
import com.tju.elm_bk.vo.OrderItemVO;
import com.tju.elm_bk.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

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
    @Operation(summary = "支付或取消订单", description = "只允许目标状态1（待商家接单）或8（已取消）；配送流转请使用/api/v1专用接口")
    public HttpResult<Long> setOrderStatus(@RequestParam Integer orderState,
                                           @RequestParam Long orderId,
                                           @RequestParam(required = false, defaultValue = "simulated") String paymentMethod,
                                           @RequestParam(required = false, defaultValue = "0") Integer pointsToUse,
                                           @RequestParam(required = false) Long couponId) {
        return HttpResult.success(orderService.setOrderState(orderId, orderState, paymentMethod, pointsToUse, couponId));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交订单", description = "使用幂等键避免重复点击创建多笔订单")
    public HttpResult<Long> orderSubmit(@RequestParam Long businessId,
                                        @RequestParam(required = false) Long addressId,
                                        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                        @RequestParam(required = false) String requestId,
                                        @RequestParam(required = false, defaultValue = "delivery") String serviceMode,
                                        @RequestParam(required = false) String foodIds) {
        // 兼容旧前端：没有请求头时也接受 requestId 查询参数。
        List<Long> selectedFoodIds = parseFoodIds(foodIds);
        return HttpResult.success(orderService.orderSubmit(businessId, addressId,
                idempotencyKey == null ? requestId : idempotencyKey, serviceMode, selectedFoodIds));
    }

    private List<Long> parseFoodIds(String foodIds) {
        if (foodIds == null || foodIds.trim().isEmpty()) return null;
        try {
            List<Long> ids = Arrays.stream(foodIds.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(Long::valueOf)
                    .filter(id -> id > 0)
                    .distinct()
                    .collect(Collectors.toList());
            if (ids.isEmpty()) throw new NumberFormatException("empty");
            return ids;
        } catch (NumberFormatException ex) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
    }



}
