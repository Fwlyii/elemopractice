package com.tju.elm_bk.constant;

import com.tju.elm_bk.entity.User;

/**
 * 系统角色名称的唯一定义。业务代码不应再散落字符串比较。
 */
public enum AuthorityName {
    USER,
    BUSINESS,
    RIDER,
    ADMIN;

    public boolean isGrantedTo(User user) {
        return user != null && user.getAuthorities() != null
                && user.getAuthorities().stream().anyMatch(authority -> name().equals(authority.getName()));
    }
}
