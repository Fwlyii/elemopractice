package com.tju.elm_bk.service;

import com.tju.elm_bk.constant.AuthorityName;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * JWT 登录用户的唯一解析入口。
 * 业务服务只从这里取当前身份，不信任请求体中的 userId。
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UserMapper userMapper;

    public User requireUser() {
        String username = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED));
        User user = loadActiveUser(username).orElse(null);
        if (user == null || Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new APIException(ResultCodeEnum.USER_MISSED);
        }
        return user;
    }

    /**
     * 公共查询页可以匿名访问；如果已登录，则返回同一套解析后的活跃用户。
     */
    public Optional<User> optionalUser() {
        return SecurityUtils.getCurrentUsername()
                .filter(username -> !"anonymousUser".equals(username))
                .flatMap(this::loadActiveUser);
    }

    public Long requireUserId() {
        return requireUser().getId();
    }

    public boolean has(User user, AuthorityName authority) {
        return authority.isGrantedTo(user);
    }

    public boolean isAdmin(User user) {
        return has(user, AuthorityName.ADMIN);
    }

    private Optional<User> loadActiveUser(String username) {
        return Optional.ofNullable(userMapper.findByUsernameWithAuthorities(username))
                .filter(user -> !Boolean.TRUE.equals(user.getIsDeleted()));
    }
}
