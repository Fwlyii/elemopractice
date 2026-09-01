package com.tju.elm_bk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FoodItemVO {
    @Schema(description = "食品ID")
    private Long id;

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

    @Schema(description = "所属商家名")
    private String businessName;

    @Schema(description = "是否上架 0-已下架 1-已上架")
    private Integer shelveStatus;
}
