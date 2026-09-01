package com.tju.elm_bk.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BusinessDTO {
    @Schema(description = "店铺ID")
    private Long id;

//    @Schema(description = "创建时间")
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    private LocalDateTime createTime;
//    @Schema(description = "更新时间")
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    private LocalDateTime updateTime;
    @Schema(description = "创建人ID")
    private Integer creator;
    @Schema(description = "更新人ID")
    private Integer updater;
    @Schema(description = "删除状态")
    private Boolean deleted;

    @Schema(description = "店铺名")
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
    private Double startPrice;
    @Schema(description = "配送价")
    private Double deliveryPrice;
    @Schema(description = "备注")
    private String remarks;
//    @Schema(description = "商铺状态")
//    private Integer status;
}
