package com.tju.elm_bk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewCreateDTO {
    @NotNull(message = "订单不能为空")
    private Long orderId;
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为1星")
    @Max(value = 5, message = "评分最高为5星")
    private Integer rating;
    @Size(max = 500, message = "评价不能超过500字")
    private String content;
    @Size(max = 1000, message = "图片信息过长")
    private String images;
}
