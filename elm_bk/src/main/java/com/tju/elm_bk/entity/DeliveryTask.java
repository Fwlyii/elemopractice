package com.tju.elm_bk.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryTask {
    private Long id;
    private Long orderId;
    private Long riderUserId;
    private String taskStatus;
    private Integer version;
    private BigDecimal distanceKm;
    private BigDecimal riderFee;
    private LocalDateTime acceptedTime;
    private LocalDateTime arrivedStoreTime;
    private LocalDateTime pickupTime;
    private LocalDateTime deliveredTime;
    private LocalDateTime completedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
