package com.tju.elm_bk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RiderApplicationDTO {
    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 40, message = "姓名不能超过40个字符")
    private String realName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "请选择配送工具")
    @Pattern(regexp = "^(E_BIKE|BIKE|WALK)$", message = "配送工具不支持")
    private String vehicleType;
}
