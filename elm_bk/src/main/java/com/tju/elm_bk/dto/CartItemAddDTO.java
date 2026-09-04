package com.tju.elm_bk.dto;

import com.tju.elm_bk.constant.PurchaseRules;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 购物车新增请求。用户、商家和价格都由服务端根据登录态与商品记录确定。
 */
@Data
public class CartItemAddDTO {
    @NotNull
    @Schema(description = "商品 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long foodId;

    @NotNull
    @Min(1)
    @Max(PurchaseRules.MAX_QUANTITY_PER_ITEM)
    @Schema(description = "本次增加的数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;
}
