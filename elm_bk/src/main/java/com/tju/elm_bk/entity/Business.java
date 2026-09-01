package com.tju.elm_bk.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Business {
    @Schema(description = "商铺ID")
    private Long id;

    @Schema(description = "商铺名称")
    private String businessName;

    @Schema(description = "商铺地址")
    private String businessAddress;

    @Schema(description = "商铺介绍")
    private String businessExplain;

    @Schema(description = "商铺图片")
    private String businessImg;

    @Schema(description = "配送费")
    private BigDecimal deliveryPrice;

    @Schema(description = "起送价")
    private BigDecimal startPrice;

    @Schema(description = "订单类型ID")
    private Integer orderTypeId;

    @Schema(description = "备注")
    private String remarks;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "创建人ID")
    private Long creator;

    @Schema(description = "是否删除")
    @JsonProperty("deleted")
    private Boolean is_deleted;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "更新人ID")
    private Long updater;

    @Schema(description = "所属用户ID")
    private Long userId;

    // 关联字段
    @Schema(description = "所属用户")
    private User user;

    @Schema(description = "商铺的商品列表")
    private List<Food> foods;

    @Schema(description = "商铺的订单列表")
    private List<Order> orders;

    @Schema(description = "商铺的购物车列表")
    private List<Cart> carts;

    @Schema(description = "商铺状态，0待审核，1已通过，2已拒绝")
    private Integer status;

}