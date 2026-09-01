package com.tju.elm_bk.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.vo.AddressVO;
import com.tju.elm_bk.vo.BusinessVO;
import com.tju.elm_bk.vo.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {
    @Schema(description = "订单ID")
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

    @Schema(description = "顾客信息对象")
    private UserVO customer;

    @Schema(description = "所属商家")
    private BusinessVO business;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "下单时间")
    private LocalDateTime orderDate;

    @Schema(description = "订单总金额")
    private BigDecimal orderTotal;

    @Schema(description = "订单状态（0-待支付，1-已支付，2-已取消，3-已完成）")
    private Integer orderState;

    @Schema(description = "地址对象")
    private AddressVO deliveryAddress;

    public Boolean verify() {
        if(business == null || business.getId() == null || deliveryAddress == null || deliveryAddress.getId() == null || customer == null  || customer.getUsername() == null) {
            return false;
        }
        return true;
    }
}
