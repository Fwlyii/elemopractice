package com.tju.elm_bk.mapper;

import com.tju.elm_bk.entity.UserPreference;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PreferenceMapper {
    @Select("SELECT id,user_id,theme,spicy_level,taste_tags,avoid_tags,category_tags,update_time FROM user_preference WHERE user_id=#{userId}")
    UserPreference findByUserId(@Param("userId") Long userId);

    @Insert("INSERT INTO user_preference(user_id,theme,spicy_level,taste_tags,avoid_tags,category_tags,update_time) VALUES(#{userId},#{theme},#{spicyLevel},#{tasteTags},#{avoidTags},#{categoryTags},NOW()) ON DUPLICATE KEY UPDATE theme=VALUES(theme),spicy_level=VALUES(spicy_level),taste_tags=VALUES(taste_tags),avoid_tags=VALUES(avoid_tags),category_tags=VALUES(category_tags),update_time=NOW()")
    int upsert(UserPreference preference);

    @Delete("DELETE FROM user_preference WHERE user_id=#{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
