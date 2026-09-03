package com.tju.elm_bk.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiderProfile {
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private String vehicleType;
    private Integer auditStatus;
    private Boolean online;
    private String rejectReason;
    private Integer completedOrders;
    private BigDecimal totalDistance;
    private BigDecimal totalIncome;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
