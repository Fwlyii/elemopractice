package com.tju.elm_bk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.entity.Food;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.vo.BusinessVO;
import com.tju.elm_bk.vo.FoodVO;
import com.tju.elm_bk.vo.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemCreateDTO {
    @Schema(description = "购物车ID")
    private Long id;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人ID")
    private Long creator;

    @Schema(description = "更新人ID")
    private Long updater;

    @JsonProperty("deleted")
    @Schema(description = "是否删除")
    private Boolean isDeleted;

    @Schema(description = "所属客户")
    private UserVO customer;

    @Schema(description = "所属商家")
    private BusinessVO business;

    @Schema(description = "商品信息")
    private FoodVO food;

    @Schema(description = "商品数量")
    private Integer quantity;

    public Boolean verify() {
        return business != null && business.getId() != null && food != null && food.getId() != null && quantity != null && customer != null && customer.getUsername() != null;
    }

}
