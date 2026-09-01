package com.tju.elm_bk.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiChatHistory {
    
    private Long id;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    private Long creator;
    
    private Boolean isDeleted;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    private Long updater;
    
    // 业务字段
    private Long userId;
    
    private String sessionId;
    
    private String userMessage;
    
    private String aiResponse;
    
    private String chatType;
    
    private Long processingTime; // 处理耗时（毫秒）
    
    private String contextData; // JSON格式存储上下文数据
}
