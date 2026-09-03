package com.tju.elm_bk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeliveryExceptionResolveDTO {
    @NotBlank(message = "处理动作不能为空")
    @Pattern(regexp = "^(RESUME|REASSIGN|CANCEL)$", message = "处理动作不支持")
    private String action;

    @Size(max = 300, message = "处理说明不能超过300个字符")
    private String note;
}
