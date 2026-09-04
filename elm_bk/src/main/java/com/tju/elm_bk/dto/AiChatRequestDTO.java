package com.tju.elm_bk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiChatRequestDTO {
    
    @Schema(description = "用户消息内容", required = true)
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 1000, message = "单次消息不能超过1000个字符")
    private String message;
    
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "由服务端从登录态注入的用户 ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long userId;
    
    @Schema(description = "会话ID，用于保持对话上下文")
    @Size(max = 64, message = "会话标识不能超过64个字符")
    private String sessionId;
    
    @Schema(description = "对话类型：general-通用对话, order-订单相关, business-商家相关, food-菜品相关")
    @Pattern(regexp = "^(general|order|business|food)$", message = "对话类型不支持")
    private String chatType = "general";
}
