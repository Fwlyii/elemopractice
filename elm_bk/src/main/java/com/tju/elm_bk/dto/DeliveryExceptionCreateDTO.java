package com.tju.elm_bk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeliveryExceptionCreateDTO {
    @NotBlank(message = "异常类型不能为空")
    @Pattern(regexp = "^(STORE_DELAY|CUSTOMER_UNREACHABLE|ADDRESS_ERROR|VEHICLE_FAILURE|OTHER)$", message = "异常类型不支持")
    private String exceptionType;

    @NotBlank(message = "异常说明不能为空")
    @Size(max = 300, message = "异常说明不能超过300个字符")
    private String description;
}
