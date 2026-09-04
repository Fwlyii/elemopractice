package com.tju.elm_bk.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "商家更新DTO")
public class BusinessUpdateDTO {
    @Schema(description = "店铺ID")
    @NotNull(message = "店铺ID不能为空")
    private Long id;

//    @Schema(description = "创建时间")
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    private LocalDateTime createTime;
//    @Schema(description = "更新时间")
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    private LocalDateTime updateTime;
    @Schema(description = "创建人ID")
    private Long creator;
    @Schema(description = "更新人ID")
    private Long updater;
    @Schema(description = "删除状态")
    private Boolean deleted;

    @Schema(description = "用户名")
    private String businessName;

    @Valid
    @NotNull(message = "店铺所有者不能为空")
    private BusinessOwnerDTO businessOwner;//级联验证。。。。别忘了改别处

    @Schema(description = "店铺地址")
    private String businessAddress;
    @Schema(description = "店铺介绍")
    private String businessExplain;
    @Schema(description = "店铺图片")
    private String businessImg;
    @Schema(description = "订单类型ID")
    private Integer orderTypeId;
    @Schema(description = "起送价")
    private BigDecimal startPrice;
    @Schema(description = "配送价")
    private BigDecimal deliveryPrice;
    @Schema(description = "备注")
    private String remarks;
    @Schema(description = "是否支持堂食")
    private Boolean dineInAvailable;
    @Schema(description = "满减门槛")
    private BigDecimal promotionThreshold;
    @Schema(description = "满减优惠金额")
    private BigDecimal promotionDiscount;
}
