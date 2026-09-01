// UserMapper.xml.java
package com.tju.elm_bk.mapper;

import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM users WHERE id = #{id} AND is_deleted = 0")
    User findById(Long id);
    @Select("SELECT * FROM users WHERE username = #{username} AND is_deleted = 0")
    User findByUsername(String username);
    User findByUsernameWithAuthorities(String username);
    User findByUserIdWithAuthorities(Long userId);
    void insert(User user);
    void update(User user);
    @Select("SELECT COUNT(*) FROM users WHERE is_deleted = 0")
    int count();
    @Insert("INSERT INTO user_authority (user_id, authority_name) VALUES (#{userId}, #{authorityName})")
    void insertUserAuthority(@Param("userId") Long userId, @Param("authorityName") String authorityName);

    @Select("select id from users where username = #{username}")
    Long getUserIdByUsername(String username);

    @Select("SELECT * FROM users WHERE id = #{userId} AND is_deleted = 0")
    Integer countUserById(@Param("userId") Long userId);

    @Update("UPDATE users SET activated = #{activated} WHERE id = #{id}")
    void updateActivated(User user);
}