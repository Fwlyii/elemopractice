package com.tju.elm_bk.service;

import com.tju.elm_bk.dto.RiderApplicationDTO;
import com.tju.elm_bk.dto.RiderAuditDTO;
import com.tju.elm_bk.entity.RiderProfile;

import java.util.List;

public interface RiderService {
    RiderProfile apply(RiderApplicationDTO dto);

    RiderProfile getMyProfile();

    RiderProfile setOnline(Boolean online);

    List<RiderProfile> listApplications(Integer auditStatus);

    RiderProfile audit(Long applicationId, RiderAuditDTO dto);
}
