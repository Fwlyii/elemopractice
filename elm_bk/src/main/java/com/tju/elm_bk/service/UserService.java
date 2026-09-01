package com.tju.elm_bk.service;

import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.vo.UserVO;

import java.util.Optional;

public interface UserService {
    User getUserWithAuthorities(String username);

    void addUser(User user);

    void updateUser(User user);

    boolean isEmptyUserTable();

    User findByUsername(String username);

    UserVO changeUserStatus(String username);

    void deleteUser(String username);

    void toggleUserActivated(String username, Boolean activated);
}