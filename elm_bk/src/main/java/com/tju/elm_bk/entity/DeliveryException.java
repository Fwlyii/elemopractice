package com.tju.elm_bk.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryException {
    private Long id;
    private Long taskId;
    private Long riderUserId;
    private String exceptionType;
    private String description;
    private String previousTaskStatus;
    private Integer status;
    private String resolutionAction;
    private String resolutionNote;
    private Long resolverUserId;
    private LocalDateTime createTime;
    private LocalDateTime resolvedTime;
}
