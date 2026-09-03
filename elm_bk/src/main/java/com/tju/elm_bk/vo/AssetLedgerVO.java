package com.tju.elm_bk.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetLedgerVO {
    private Long id;
    private String type;
    private BigDecimal amount;
    private Integer pointsDelta;
    private String reason;
    private LocalDateTime createTime;
}
