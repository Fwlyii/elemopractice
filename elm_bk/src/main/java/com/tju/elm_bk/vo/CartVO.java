package com.tju.elm_bk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartVO {
    @Schema(description = "购物车ID")
    private Long id;

    @Schema(description = "商品数量")
    private Integer quantity;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人ID")
    private Long creator;

    @Schema(description = "是否删除")
    private Boolean isDeleted;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "更新人ID")
    private Long updater;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "商家ID")
    private Long businessId;

    @Schema(description = "商品ID")
    private Long foodId;


    @Schema(description = "所属客户")
    private UserVO customer;

    @Schema(description = "所属商家")
    private BusinessVO business;

    @Schema(description = "商品信息")
    private FoodVO food;
}
