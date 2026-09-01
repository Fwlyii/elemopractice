package com.tju.elm_bk.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemVO {
    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单总金额")
    private BigDecimal orderTotal;

    @Schema(description = "订单状态(0-待支付,1-待接单,2-已接单,3-已完成,4-已取消)")
    private Integer orderState;

    @Schema(description = "下单时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime orderDate;

    @Schema(description = "商家ID")
    private Long businessId;

    @Schema(description = "商家名")
    private String businessName;

    @Schema(description = "商家图片")
    private String businessImg;

}
