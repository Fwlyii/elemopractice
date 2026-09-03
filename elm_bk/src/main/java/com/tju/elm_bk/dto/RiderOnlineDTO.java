package com.tju.elm_bk.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RiderOnlineDTO {
    @NotNull(message = "在线状态不能为空")
    private Boolean online;
}
