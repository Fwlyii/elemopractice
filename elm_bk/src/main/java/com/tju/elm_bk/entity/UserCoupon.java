package com.tju.elm_bk.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserCoupon {
    private Long id;
    private Long userId;
    private String name;
    private BigDecimal discountAmount;
    private BigDecimal minOrderAmount;
    private LocalDateTime expiresAt;
    private Boolean used;
    private Long orderId;
}
