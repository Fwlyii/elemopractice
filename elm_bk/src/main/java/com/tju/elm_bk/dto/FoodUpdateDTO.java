package com.tju.elm_bk.dto;

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

    public boolean verify() {
        return foodId != null && foodPrice != null && foodPrice.compareTo(BigDecimal.ZERO) > 0;
    }
}
