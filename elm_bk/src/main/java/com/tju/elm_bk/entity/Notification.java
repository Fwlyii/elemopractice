package com.tju.elm_bk.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Notification {
    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "消息类型：0=商家申请审核，1=开店申请审核")
    private Integer notificationType;

    @Schema(description = "消息内容")
    private String notificationContent;

    @Schema(description = "审核结果：1=通过，2=拒绝")
    private Integer auditResult;

    @Schema(description = "是否已读：0=未读，1=已读")
    private Integer isRead;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "阅读时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;

    @Schema(description = "删除状态")
    private Integer isDeleted;
}
