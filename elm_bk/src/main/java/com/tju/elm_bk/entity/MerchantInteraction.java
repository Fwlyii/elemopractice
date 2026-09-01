package com.tju.elm_bk.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;
import io.swagger.v3.oas.annotations.media.Schema;
// MerchantInteraction.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantInteraction {
    @Schema(description = "商户互动ID")
    private Long id;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "商户ID")
    private Long merchantId;
    @Schema(description = "是否点赞")
    private Boolean liked;
    @Schema(description = "是否收藏")
    private Boolean collected;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}