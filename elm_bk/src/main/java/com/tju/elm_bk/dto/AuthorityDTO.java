package com.tju.elm_bk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "权限DTO")
public class AuthorityDTO {
    @Schema(description = "权限ID")
    private String name;

}