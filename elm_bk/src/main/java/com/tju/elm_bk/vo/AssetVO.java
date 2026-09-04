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
    /** 新人券是否曾经领取过；即使已经使用或过期也为 true。 */
    private boolean welcomeCouponClaimed;
}
