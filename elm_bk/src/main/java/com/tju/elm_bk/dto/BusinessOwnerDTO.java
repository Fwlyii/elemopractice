package com.tju.elm_bk.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tju.elm_bk.entity.Authority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "商家所有者DTO")
public class BusinessOwnerDTO {
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    @Size(min = 1, max = 100, message = "用户名长度必须在1-100个字符之间")
    private String username;

//    @Schema(description = "创建时间")
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    private LocalDateTime createTime;

    @Schema(description = "创建人ID")
    private Integer creator;

    @Schema(description = "是否删除")
    @JsonProperty("deleted")
    private Boolean isDeleted;

//    @Schema(description = "更新时间")
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    private LocalDateTime updateTime;

    @Schema(description = "更新人ID")
    private Integer updater;

    @Schema(description = "用户权限")
    private List<Authority> authorities;
}