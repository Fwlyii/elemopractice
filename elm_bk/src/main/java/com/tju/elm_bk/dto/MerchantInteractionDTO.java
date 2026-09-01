package com.tju.elm_bk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MerchantInteractionDTO {
    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID")
    private Long userId;

    @NotNull(message = "商铺ID不能为空")
    @Schema(description = "商铺ID")
    private Long merchantId;

    @NotNull(message = "是否点赞,0没点赞，1点赞了")
    private Boolean liked;
    @NotNull(message = "是否收藏,0没收藏，1收藏了")
    private Boolean collected;
}