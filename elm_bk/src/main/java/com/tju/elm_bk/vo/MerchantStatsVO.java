package com.tju.elm_bk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MerchantStatsVO {
    @Schema(description = "商铺ID")
    private Long merchantId;
    @Schema(description = "商铺名")
    private String merchantName;
    @Schema(description = "商铺点赞数")
    private Integer likeCount;
    @Schema(description = "商铺收藏数")
    private Integer collectCount;
    @Schema(description = "商铺评分")
    private BigDecimal rating;
}