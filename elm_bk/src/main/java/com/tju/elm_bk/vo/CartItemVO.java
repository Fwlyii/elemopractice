package com.tju.elm_bk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemVO {
    @Schema(description = "购物车ID")
    private Long id;

    @Schema(description = "商品数量")
    private Integer quantity;

    @Schema(description = "商家ID")
    private Long businessId;

    @Schema(description = "商家名")
    private String businessName;

    @Schema(description = "商品ID")
    private Long foodId;

    @Schema(description = "商品图片")
    private String foodImg;

    @Schema(description = "商品名")
    private String foodName;

    @Schema(description = "商品单价")
    private Double foodPrice;
}
