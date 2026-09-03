package com.tju.elm_bk.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetVO {
    private BigDecimal balance;
    private Integer points;
    private LocalDateTime membershipExpire;
    private boolean member;
    private int availableCoupons;
}
