package com.tju.elm_bk.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryExceptionVO {
    private Long id;
    private Long taskId;
    private Long orderId;
    private Long riderUserId;
    private String riderName;
    private String exceptionType;
    private String description;
    private String previousTaskStatus;
    private Integer status;
    private String resolutionAction;
    private String resolutionNote;
    private String businessName;
    private String customerName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedTime;
}
