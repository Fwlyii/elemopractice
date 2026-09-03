package com.tju.elm_bk.dto;

import lombok.Data;

/** 偏好只影响展示与推荐，不参与历史订单计算。 */
@Data
public class PreferenceUpdateDTO {
    private String theme;
    private Integer spicyLevel;
    private String tasteTags;
    private String avoidTags;
    private String categoryTags;
}
