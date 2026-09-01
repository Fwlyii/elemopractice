package com.tju.elm_bk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderFoodVO {
    @Schema(description = "订单详情ID")
    private Long id;

    @Schema(description = "商品数量")
    private Integer quantity;

    @Schema(description = "所属订单ID")
    private Long orderId;

    @Schema(description = "商品ID")
    private Long foodId;

    @Schema(description = "商品名")
    private String foodName;

    @Schema(description = "商品单价")
    private BigDecimal foodPrice;
}
