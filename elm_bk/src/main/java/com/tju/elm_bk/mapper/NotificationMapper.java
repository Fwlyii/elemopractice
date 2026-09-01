package com.tju.elm_bk.mapper;

import com.tju.elm_bk.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NotificationMapper {
    void insert(Notification notification);

    @Select("SELECT * FROM notification WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY create_time DESC;")
    List<Notification> list(Long userId);

    @Update("UPDATE notification SET is_read =1,read_time = #{readTime} WHERE id = #{id}")
    void updateRead(Long id, LocalDateTime readTime);
}
