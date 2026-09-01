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
public class AiChatResponseVO {
    
    @Schema(description = "AI回复消息内容")
    private String message;
    
    @Schema(description = "会话ID")
    private String sessionId;
    
    @Schema(description = "回复类型：text-文本, suggestion-建议, data-数据展示")
    private String responseType = "text";
    
    @Schema(description = "相关数据，当responseType为data时使用")
    private Object relatedData;
    
    @Schema(description = "是否需要用户确认")
    private Boolean needConfirmation = false;
    
    @Schema(description = "确认操作类型")
    private String confirmationType;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "响应时间")
    private LocalDateTime responseTime;
    
    @Schema(description = "处理耗时（毫秒）")
    private Long processingTime;
}
