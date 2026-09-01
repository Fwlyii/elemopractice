package com.tju.elm_bk.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tju.elm_bk.vo.BusinessVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FoodDTO {
    @Schema(description = "食品ID")
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
    private Boolean deleted;

    @Schema(description = "食品名称")
    private String foodName;

    @Schema(description = "食品说明")
    private String foodExplain;

    @Schema(description = "食品图片")
    private String foodImg;

    @Schema(description = "食品价格")
    private BigDecimal foodPrice;

    @Schema(description = "备注")
    private String remarks;

    @Schema(description = "所属商家")
    private BusinessVO business;

    public Boolean verify() {
        if(business == null || business.getId() == null || foodName == null ||foodPrice == null || foodPrice.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        return true;
    }
}
