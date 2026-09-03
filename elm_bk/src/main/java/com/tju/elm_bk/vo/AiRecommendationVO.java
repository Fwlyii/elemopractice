package com.tju.elm_bk.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendationVO {
    private Long foodId;
    private String foodName;
    private BigDecimal price;
    private String foodImg;
    private Long businessId;
    private String businessName;
    private String reason;
}
