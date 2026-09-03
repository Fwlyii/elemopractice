package com.tju.elm_bk.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RiderAuditDTO {
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    @Size(max = 200, message = "审核说明不能超过200个字符")
    private String reason;
}
