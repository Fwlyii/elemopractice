package com.tju.elm_bk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

    private String businessAddress;
    private Integer orderTypeId;
    private Boolean dineInAvailable;
    private BigDecimal promotionThreshold;
    private BigDecimal promotionDiscount;
    private BigDecimal averagePrice;
    private LocalDateTime createTime;

    /** 后端规则引擎生成的候选展示标签，前端只负责渲染。 */
    private List<String> recommendationTags;

    /** 用于综合排序的后端推荐分，避免把业务门槛散落在前端。 */
    private BigDecimal recommendationScore;

    /** 兼容互动服务中对基础搜索结果的构造方式。 */
    public BusinessSearchVO(Long id, String businessName, String businessImg,
                            BigDecimal startPrice, BigDecimal deliveryPrice,
                            BigDecimal score, Integer salesCount) {
        this.id = id;
        this.businessName = businessName;
        this.businessImg = businessImg;
        this.startPrice = startPrice;
        this.deliveryPrice = deliveryPrice;
        this.score = score;
        this.salesCount = salesCount;
    }
}
