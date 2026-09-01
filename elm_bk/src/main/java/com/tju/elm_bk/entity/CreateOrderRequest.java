package com.tju.elm_bk.entity;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    @Schema(description = "用户id")
    private String userId;
    @Schema(description = "商家id")
    private Integer businessId;
    @Schema(description = "送货地址id")
    private Integer daId;
    @Schema(description = "商品总数")
    private Double orderTotal;

}