package com.tju.elm_bk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "首页搜索与评分筛选所得商铺信息")
public class BusinessSearchVO {
    @Schema(description = "商铺ID")
    private Long id;
    @Schema(description = "商铺名")
    private String businessName;
    @Schema(description = "商铺图片")
    private String businessImg;

    @Schema(description = "起送价")
    private BigDecimal startPrice;

    @Schema(description = "配送费")
    private BigDecimal deliveryPrice;
    @Schema(description = "评分")
    private BigDecimal  score;
    @Schema(description = "销量")
    private Integer salesCount;
}
