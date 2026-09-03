package com.tju.elm_bk.controller;

import com.tju.elm_bk.entity.OrderStatusHistory;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.service.DeliveryService;
import com.tju.elm_bk.vo.DeliveryTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "订单配送衔接")
public class DeliveryOrderController {
    private final DeliveryService deliveryService;

    public DeliveryOrderController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PostMapping("/{id}/merchant-accept")
    @PreAuthorize("hasAuthority('BUSINESS')")
    @Operation(summary = "商家接单并生成配送任务")
    public HttpResult<DeliveryTaskVO> merchantAccept(@PathVariable Long id) {
        return HttpResult.success(deliveryService.merchantAcceptOrder(id));
    }

    @PostMapping("/{id}/merchant-ready")
    @PreAuthorize("hasAuthority('BUSINESS')")
    public HttpResult<DeliveryTaskVO> merchantReady(@PathVariable Long id) {
        return HttpResult.success(deliveryService.merchantReadyOrder(id));
    }

    @PostMapping("/{id}/merchant-reject")
    @PreAuthorize("hasAuthority('BUSINESS')")
    public HttpResult<Void> merchantReject(@PathVariable Long id) {
        deliveryService.merchantRejectOrder(id);
        return HttpResult.success();
    }

    @PostMapping("/{id}/confirm-receipt")
    @PreAuthorize("hasAuthority('USER')")
    public HttpResult<DeliveryTaskVO> confirmReceipt(@PathVariable Long id) {
        return HttpResult.success(deliveryService.confirmReceipt(id));
    }

    @GetMapping("/{id}/delivery")
    public HttpResult<DeliveryTaskVO> delivery(@PathVariable Long id) {
        return HttpResult.success(deliveryService.getOrderDelivery(id));
    }

    @GetMapping("/{id}/status-history")
    public HttpResult<List<OrderStatusHistory>> statusHistory(@PathVariable Long id) {
        return HttpResult.success(deliveryService.listOrderHistory(id));
    }
}
