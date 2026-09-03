package com.tju.elm_bk.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class Order {
    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单总金额")
    private BigDecimal orderTotal;

    @Schema(description = "订单状态：0待支付、1待商家接单、2制作中、3待骑手接单、4待取餐、5配送中、6已送达、7已完成、8已取消、9配送异常")
    private Integer orderState;

    @Schema(description = "下单时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderDate;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "创建人ID")
    private Long creator;

    @Schema(description = "是否删除")
    private Boolean isDeleted;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "更新人ID")
    private Long updater;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "商家ID")
    private Long businessId;

    @Schema(description = "地址ID")
    private Long addressId;

    @Schema(description = "订单配送费")
    private BigDecimal deliveryPrice;

    @Schema(description = "支付方式：SIMULATED 或 WALLET")
    private String paymentMethod;

    @Schema(description = "本单抵扣积分")
    private Integer pointsUsed;

    @Schema(description = "是否使用钱包支付")
    private Boolean walletPaid;

    @Schema(description = "支付状态：PENDING、PAID、REFUNDED、CANCELLED")
    private String paymentStatus;

    /** 顾客提交订单时生成的幂等键，防止重复点击产生多笔订单。 */
    private String idempotencyKey;

    @Schema(description = "履约方式：DELIVERY 外送、PICKUP 自取")
    private String serviceMode;

    @Schema(description = "下单时保存的收货地址快照")
    private String addressSnapshot;

    private String contactNameSnapshot;
    private Integer contactSexSnapshot;
    private String contactTelSnapshot;

//    // 关联字段
//    @Schema(description = "下单客户")
//    private User customer;
//
//    @Schema(description = "所属商家")
//    private Business business;
//
//    @Schema(description = "配送地址")
//    private DeliveryAddress deliveryAddress;
//
//    @Schema(description = "订单详情列表")
//    private List<OrderDetailet> orderDetailets;
}
