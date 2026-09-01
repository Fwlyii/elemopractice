package com.tju.elm_bk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiChatRequestDTO {
    
    @Schema(description = "用户消息内容", required = true)
    @NotBlank(message = "消息内容不能为空")
    private String message;
    
    @Schema(description = "用户ID，用于获取用户相关信息")
    private Long userId;
    
    @Schema(description = "会话ID，用于保持对话上下文")
    private String sessionId;
    
    @Schema(description = "对话类型：general-通用对话, order-订单相关, business-商家相关, food-菜品相关")
    private String chatType = "general";
}
