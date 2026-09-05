package com.tju.elm_bk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 登录专用请求。登录端是认证上下文的一部分，不能只由前端在拿到令牌后判断。
 */
public class AuthenticationDTO {

    @NotBlank
    @Size(max = 100)
    private String username;

    @NotBlank
    @Size(max = 128)
    private String password;

    @NotBlank
    @Pattern(regexp = "user|merchant|rider|admin")
    private String role;

    private Boolean rememberMe;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
}
