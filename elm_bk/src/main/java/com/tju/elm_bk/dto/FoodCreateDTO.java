package com.tju.elm_bk.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FoodCreateDTO {
    @Schema(description = "食品名称")
    private String foodName;

    @Schema(description = "食品价格")
    private BigDecimal foodPrice;

    @Schema(description = "食品说明")
    private String foodExplain;

    @Schema(description = "食品图片")
    private String foodImg;

    @Schema(description = "备注")
    private String remarks;

    @Schema(description = "所属商家ID")
    private Long businessId;

    @Schema(description = "可售库存，默认100")
    private Integer stock;

    @Schema(description = "商家自定义的商品分类，空值归入其他")
    private String category;

    @Schema(description = "单笔限购数量，空表示不限购")
    private Integer purchaseLimit;


    public boolean verify() {
        return foodName != null && !foodName.trim().isEmpty() && businessId != null && foodPrice != null
                && foodPrice.compareTo(BigDecimal.ZERO) > 0 && (stock == null || stock >= 0)
                && foodPrice.compareTo(new BigDecimal("100000")) <= 0 && (stock == null || stock <= 1_000_000)
                && (purchaseLimit == null || purchaseLimit > 0);
    }

}
