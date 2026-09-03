package com.tju.elm_bk.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserAsset {
    private Long id;
    private Long userId;
    private BigDecimal balance;
    private Integer points;
    private LocalDateTime membershipExpire;
    private LocalDateTime updateTime;
}
