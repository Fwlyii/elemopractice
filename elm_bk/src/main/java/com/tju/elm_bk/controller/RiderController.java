package com.tju.elm_bk.controller;

import com.tju.elm_bk.dto.RiderApplicationDTO;
import com.tju.elm_bk.dto.RiderOnlineDTO;
import com.tju.elm_bk.entity.RiderProfile;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.service.DeliveryService;
import com.tju.elm_bk.service.RiderService;
import com.tju.elm_bk.vo.DeliveryTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/riders")
@Tag(name = "骑手管理")
public class RiderController {
    private final RiderService riderService;
    private final DeliveryService deliveryService;

    public RiderController(RiderService riderService, DeliveryService deliveryService) {
        this.riderService = riderService;
        this.deliveryService = deliveryService;
    }

    @PostMapping("/applications")
    @PreAuthorize("hasAuthority('RIDER_APPLICANT')")
    @Operation(summary = "申请成为骑手")
    public HttpResult<RiderProfile> apply(@Valid @RequestBody RiderApplicationDTO dto) {
        return HttpResult.success(riderService.apply(dto));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('RIDER_APPLICANT','RIDER')")
    @Operation(summary = "获取当前用户的骑手档案")
    public HttpResult<RiderProfile> me() {
        return HttpResult.success(riderService.getMyProfile());
    }

    @PatchMapping("/me/online")
    @PreAuthorize("hasAuthority('RIDER')")
    @Operation(summary = "骑手上线或下线")
    public HttpResult<RiderProfile> setOnline(@Valid @RequestBody RiderOnlineDTO dto) {
        return HttpResult.success(riderService.setOnline(dto.getOnline()));
    }

    @GetMapping("/me/tasks")
    @PreAuthorize("hasAuthority('RIDER')")
    @Operation(summary = "获取当前骑手的任务")
    public HttpResult<List<DeliveryTaskVO>> myTasks(@RequestParam(required = false) Boolean active) {
        return HttpResult.success(deliveryService.listMyTasks(active));
    }

    @GetMapping("/available-tasks")
    @PreAuthorize("hasAuthority('RIDER')")
    @Operation(summary = "获取可接配送任务")
    public HttpResult<List<DeliveryTaskVO>> availableTasks() {
        return HttpResult.success(deliveryService.listAvailableTasks());
    }
}
