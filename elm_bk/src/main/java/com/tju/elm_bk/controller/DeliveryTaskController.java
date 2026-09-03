package com.tju.elm_bk.controller;

import com.tju.elm_bk.dto.DeliveryExceptionCreateDTO;
import com.tju.elm_bk.entity.DeliveryException;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.service.DeliveryService;
import com.tju.elm_bk.vo.DeliveryTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/delivery-tasks")
@PreAuthorize("hasAuthority('RIDER')")
@Tag(name = "配送任务")
public class DeliveryTaskController {
    private final DeliveryService deliveryService;

    public DeliveryTaskController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/{id}")
    public HttpResult<DeliveryTaskVO> get(@PathVariable Long id) {
        return HttpResult.success(deliveryService.getTask(id));
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "原子抢单")
    public HttpResult<DeliveryTaskVO> accept(@PathVariable Long id) {
        return HttpResult.success(deliveryService.acceptTask(id));
    }

    @PostMapping("/{id}/arrive-store")
    public HttpResult<DeliveryTaskVO> arriveStore(@PathVariable Long id) {
        return HttpResult.success(deliveryService.arriveStore(id));
    }

    @PostMapping("/{id}/pickup")
    public HttpResult<DeliveryTaskVO> pickup(@PathVariable Long id) {
        return HttpResult.success(deliveryService.pickup(id));
    }

    @PostMapping("/{id}/deliver")
    public HttpResult<DeliveryTaskVO> deliver(@PathVariable Long id) {
        return HttpResult.success(deliveryService.deliver(id));
    }

    @PostMapping("/{id}/exceptions")
    public HttpResult<DeliveryException> reportException(@PathVariable Long id,
                                                         @Valid @RequestBody DeliveryExceptionCreateDTO dto) {
        return HttpResult.success(deliveryService.reportException(id, dto));
    }
}
