package com.tju.elm_bk.service;

import com.tju.elm_bk.dto.AuditPermissionDTO;
import com.tju.elm_bk.dto.BusinessDTO;
import com.tju.elm_bk.dto.BusinessPermissionDTO;
import com.tju.elm_bk.entity.PermissionApplication;
import com.tju.elm_bk.vo.BusinessPermissionVO;
import com.tju.elm_bk.vo.MerchantApplicationsVO;

import java.util.List;

public interface PermissionApplicationService {
    PermissionApplication applyMerchant();

    PermissionApplication auditApplication(AuditPermissionDTO auditDTO);

    BusinessPermissionVO applyShop(BusinessPermissionDTO businessPermissionDTO);

    BusinessPermissionVO auditShopApplication(BusinessPermissionDTO businessPermissionDTO);

    List<MerchantApplicationsVO> getMerchantApplications();

    List<BusinessPermissionVO> getShopApplications();
}
