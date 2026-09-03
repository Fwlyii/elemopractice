package com.tju.elm_bk.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceVO {
    private String theme;
    private Integer spicyLevel;
    private String tasteTags;
    private String avoidTags;
    private String categoryTags;
}
