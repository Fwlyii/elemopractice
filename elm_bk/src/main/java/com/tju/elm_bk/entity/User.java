package com.tju.elm_bk.entity;

import com.alibaba.druid.support.monitor.annotation.MTable;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "是否激活")
    private Boolean activated=true;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "创建人ID")
    private Long creator;

    @Schema(description = "是否删除")
    @JsonProperty("deleted")
    private Boolean isDeleted;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "更新人ID")
    private Long updater;

    // 关联字段
    @Schema(description = "用户拥有的权限列表")
    private List<Authority> authorities;

    @Schema(description = "用户个人信息")
    private Person person;

    @Schema(description = "用户拥有的商家列表")
    private List<Business> businesses;

    @Schema(description = "用户的配送地址列表")
    private List<DeliveryAddress> deliveryAddresses;

    @Schema(description = "用户的订单列表")
    private List<Order> orders;

    @Schema(description = "用户的购物车列表")
    private List<Cart> carts;

    public User(Long id, String username, String password, Boolean activated, LocalDateTime createTime, Long creator, Boolean isDeleted, LocalDateTime updateTime, Long updater, List<Authority> authorities){
        this.id = id;
        this.username = username;
        this.password = password;
        this.activated = activated;
        this.createTime = createTime;
        this.creator = creator;
        this.isDeleted = isDeleted;
        this.updateTime = updateTime;
        this.updater = updater;
        this.authorities = authorities;
    }
}
