
// UserServiceImpl.java
package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.service.CurrentUserService;
import com.tju.elm_bk.service.UserService;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;


    public User getUserWithAuthorities(String username) {
        return userMapper.findByUsernameWithAuthorities(username);
    }

    public void addUser(User user) {
        userMapper.insert(user);
    }

    public void updateUser(User user) {
        userMapper.update(user);
    }

    public boolean isEmptyUserTable() {
        return userMapper.count() == 0;
    }

    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public UserVO changeUserStatus(String username) {
        User currentUser = currentUserService.requireUser();
        User targetUser = userMapper.findByUsernameWithAuthorities(username);
        if (targetUser == null) {
            throw new APIException("目标用户不存在");
        }

        targetUser.setActivated(!targetUser.getActivated());
        targetUser.setUpdateTime(LocalDateTime.now()); // 更新时间
        targetUser.setUpdater(currentUser.getId()); // 更新人ID（当前管理员ID）

        userMapper.update(targetUser);

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(targetUser, userVO);
        return userVO;
    }

    @Override
    public void deleteUser(String username) {
        User targetUser = userMapper.findByUsernameWithAuthorities(username);
        User currentUser = currentUserService.requireUser();
        if (targetUser == null) {
            throw new APIException("目标用户不存在");
        }
        if (targetUser.getIsDeleted()) {
            throw new APIException("用户已被删除");
        }
        if (currentUser.getUsername().equals(username)) {
            throw new APIException("不能删除当前登录的管理员账号");
        }

        targetUser.setIsDeleted(true);
        targetUser.setUpdateTime(LocalDateTime.now());
        targetUser.setUpdater(currentUser.getId());
        userMapper.update(targetUser);
    }

    @Override
    public void toggleUserActivated(String username, Boolean activated) {
        if (username == null || username.isBlank() || activated == null) {
            throw new APIException("用户名和账号状态不能为空");
        }
        User currentUser = currentUserService.requireUser();
        if (!currentUserService.isAdmin(currentUser)) {
            throw new APIException("只有管理员可以启用或禁用账号");
        }
        if (currentUser.getUsername().equals(username)) {
            throw new APIException("不能修改当前登录管理员自己的启用状态");
        }
        User user = userMapper.findByUsername(username.trim());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setActivated(activated);
        userMapper.updateActivated(user); // 更新数据库activated字段
    }
}
