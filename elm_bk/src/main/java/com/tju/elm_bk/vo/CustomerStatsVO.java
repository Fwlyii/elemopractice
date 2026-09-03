package com.tju.elm_bk.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatsVO {
    private Integer completedOrderCount;
    private BigDecimal totalSpent;
    private BigDecimal averageOrder;
    private Integer visitedBusinessCount;
    private Integer visitedCategoryCount;
}
