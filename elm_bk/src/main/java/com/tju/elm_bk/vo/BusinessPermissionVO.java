package com.tju.elm_bk.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BusinessPermissionVO {
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

    @Schema(description = "创建人ID")
    private Long creator;

    @Schema(description = "是否删除")
    @JsonProperty("deleted")
    private Boolean isDeleted;

    @Schema(description = "更新人ID")
    private Long updater;

    @Schema(description = "所属用户ID")
    private Long userId;

    @Schema(description = "商铺的状态")
    private Integer status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
