package com.tju.elm_bk.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BusinessVO {

    @Schema(description = "商铺ID")
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人ID")
    private Long creator;

    @Schema(description = "更新人ID")
    private Long updater;

    @JsonProperty("deleted")
    @Schema(description = "是否删除")
    private Boolean isDeleted;

    @Schema(description = "商铺名称")
    private String businessName;

    @Schema(description = "商铺地址")
    private String businessAddress;

    @Schema(description = "商铺介绍")
    private String businessExplain;

    @Schema(description = "商铺图片")
    private String businessImg;

    @Schema(description = "订单类型ID")
    private Integer orderTypeId;

    @Schema(description = "起送价")
    private BigDecimal startPrice;

    @Schema(description = "配送费")
    private BigDecimal deliveryPrice;

    @Schema(description = "备注")
    private String remarks;

    @Schema(description = "所属商家用户")
    private UserVO businessOwner;

}
