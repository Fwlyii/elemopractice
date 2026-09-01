package com.tju.elm_bk.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class OrderItemDetailVO {
    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单总金额")
    private BigDecimal orderTotal;

    @Schema(description = "订单配送费")
    private BigDecimal deliveryPrice;

    @Schema(description = "订单状态(0-待支付,1-待接单,2-已结单,3-已完成,4-已取消")
    private Integer orderState;

    @Schema(description = "下单时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime orderDate;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客户姓名")
    private String customerName;

    @Schema(description = "商家ID")
    private Long businessId;

    @Schema(description = "商家名")
    private String businessName;

    @Schema(description = "商家图片")
    private String businessImg;

    @Schema(description = "地址ID")
    private Long addressId;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "联系人性别")
    private Integer contactSex;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "联系方式")
    private String contactTel;

    @Schema(description = "订单商品列表")
    private List<OrderFoodVO> foodList;
}
