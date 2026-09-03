package com.tju.elm_bk.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 顾客可跨会话恢复的轻量偏好设置。 */
@Data
public class UserPreference {
    private Long id;
    private Long userId;
    private String theme;
    private Integer spicyLevel;
    private String tasteTags;
    private String avoidTags;
    private String categoryTags;
    private LocalDateTime updateTime;
}
