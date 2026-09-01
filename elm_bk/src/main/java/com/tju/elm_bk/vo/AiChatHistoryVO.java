package com.tju.elm_bk.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiChatHistoryVO {
    
    @Schema(description = "对话记录ID")
    private Long id;
    
    @Schema(description = "用户ID")
    private Long userId;
    
    @Schema(description = "会话ID")
    private String sessionId;
    
    @Schema(description = "用户消息")
    private String userMessage;
    
    @Schema(description = "AI回复")
    private String aiResponse;
    
    @Schema(description = "对话类型")
    private String chatType;
    
    @Schema(description = "处理耗时（毫秒）")
    private Long processingTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "对话时间")
    private LocalDateTime createTime;
}
