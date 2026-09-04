package com.tju.elm_bk.dto;

import com.tju.elm_bk.constant.PurchaseRules;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FoodUpdateDTO {
    @Schema(description = "食品id")
    private Long foodId;

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

    @Schema(description = "可售库存")
    private Integer stock;

    @Schema(description = "商品分类")
    private String category;

    @Schema(description = "单笔限购数量；修改时由purchaseLimitEnabled决定启用或清除")
    private Integer purchaseLimit;

    @Schema(description = "是否启用单笔限购；false会明确清除旧限购，空值兼容旧客户端")
    private Boolean purchaseLimitEnabled;

    public boolean verify() {
        return foodId != null && foodPrice != null && foodPrice.compareTo(BigDecimal.ZERO) > 0
                && foodPrice.compareTo(new BigDecimal("100000")) <= 0 && (stock == null || (stock >= 0 && stock <= 1_000_000))
                && (!Boolean.TRUE.equals(purchaseLimitEnabled) || purchaseLimit != null)
                && (purchaseLimit == null || (purchaseLimit > 0 && purchaseLimit <= PurchaseRules.MAX_QUANTITY_PER_ITEM));
    }
}
