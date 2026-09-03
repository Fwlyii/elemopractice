package com.tju.elm_bk.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistory {
    private Long id;
    private Long orderId;
    private Integer fromStatus;
    private Integer toStatus;
    private Long operatorUserId;
    private String reason;
    private LocalDateTime createTime;
}
