package com.tju.elm_bk.controller;

import com.tju.elm_bk.dto.AuditPermissionDTO;
import com.tju.elm_bk.dto.BusinessDTO;
import com.tju.elm_bk.dto.BusinessPermissionDTO;
import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.entity.PermissionApplication;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.service.BusinessService;
import com.tju.elm_bk.service.PermissionApplicationService;
import com.tju.elm_bk.vo.BusinessPermissionVO;
import com.tju.elm_bk.vo.MerchantApplicationsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permission")
@Tag(name = "权限申请管理", description = "顾客申请成为商家与申请开店")
public class PermissionApplicationController {
    @Autowired
    private PermissionApplicationService permissionApplicationService;
    /**
     * 顾客申请成为商家
     */
    @PostMapping("/apply-merchant")
    @Operation(summary = "申请成为商家", description = "顾客提交成为商家的申请，提交后会通知管理员审核")
    public HttpResult<PermissionApplication> applyMerchant() {
        return HttpResult.success(permissionApplicationService.applyMerchant());
    }

    /**
     * 审核用户的商家申请（同意/拒绝）
     */
    @PostMapping("/audit")
    @Operation(summary = "审核商家申请", description = "管理员审核用户的商家申请，同意后将添加BUSINESS权限并通知用户")
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<PermissionApplication> auditApplication(@Valid @RequestBody AuditPermissionDTO auditDTO) {
        return HttpResult.success(permissionApplicationService.auditApplication(auditDTO));
    }

    /**
     * 顾客申请开店
     */
    @PostMapping("/apply-shop")
    @Operation(summary = "申请开店", description = "顾客提交开店的申请，提交后会通知管理员审核")
    public HttpResult<BusinessPermissionVO> applyShop(@RequestBody BusinessPermissionDTO businessPermissionDTO) {
        return HttpResult.success(permissionApplicationService.applyShop(businessPermissionDTO));
    }

    /**
     * 审核用户的开店申请（同意/拒绝）
     */
    @PostMapping("/audit-shop")
    @Operation(summary = "审核开店申请", description = "管理员审核用户的开店申请")
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<BusinessPermissionVO> auditShop(@RequestBody BusinessPermissionDTO businessPermissionDTO) {
        return HttpResult.success(permissionApplicationService.auditShopApplication(businessPermissionDTO));
    }

    @GetMapping("/merchant-applications")
    @Operation(summary = "获取申请成为商家的待审核列表", description = "获取申请成为商家的待审核列表")
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<List<MerchantApplicationsVO>> getMerchantApplications() {
        return HttpResult.success(permissionApplicationService.getMerchantApplications());
    }

    @GetMapping("/shop-applications")
    @Operation(summary = "获取申请开店的待审核列表", description = "获取申请开店的待审核列表")
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<List<BusinessPermissionVO>> getShopApplications() {
        return HttpResult.success(permissionApplicationService.getShopApplications());
    }
}
