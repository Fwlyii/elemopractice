package com.tju.elm_bk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MerchantInteractionVO {
    @Schema(description = "商铺ID")
    private Long merchantId;
    @Schema(description = "商铺名")
    private String merchantName;
    @Schema(description = "是否点赞")
    private Boolean liked;
    @Schema(description = "是否收藏")
    private Boolean collected;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
