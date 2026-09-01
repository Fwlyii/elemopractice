package com.tju.elm_bk.mapper;

import com.tju.elm_bk.entity.AiChatHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiChatHistoryMapper {
    
    /**
     * 插入AI对话记录
     */
    @Insert("INSERT INTO ai_chat_history (create_time, creator, is_deleted, update_time, updater, " +
            "user_id, session_id, user_message, ai_response, chat_type, processing_time, context_data) " +
            "VALUES (#{createTime}, #{creator}, #{isDeleted}, #{updateTime}, #{updater}, " +
            "#{userId}, #{sessionId}, #{userMessage}, #{aiResponse}, #{chatType}, #{processingTime}, #{contextData})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiChatHistory aiChatHistory);
    
    /**
     * 根据ID查询对话记录
     */
    @Select("SELECT * FROM ai_chat_history WHERE id = #{id} AND is_deleted = 0")
    AiChatHistory selectById(Long id);
    
    /**
     * 根据用户ID查询对话记录
     */
    @Select("SELECT * FROM ai_chat_history WHERE user_id = #{userId} AND is_deleted = 0 " +
            "ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<AiChatHistory> selectByUserId(@Param("userId") Long userId, 
                                       @Param("limit") Integer limit, 
                                       @Param("offset") Integer offset);
    
    /**
     * 根据会话ID查询对话记录
     */
    @Select("SELECT * FROM ai_chat_history WHERE session_id = #{sessionId} AND is_deleted = 0 " +
            "ORDER BY create_time ASC")
    List<AiChatHistory> selectBySessionId(String sessionId);
    
    /**
     * 根据用户ID和对话类型查询最近的对话记录
     */
    @Select("SELECT * FROM ai_chat_history WHERE user_id = #{userId} AND chat_type = #{chatType} " +
            "AND is_deleted = 0 ORDER BY create_time DESC LIMIT #{limit}")
    List<AiChatHistory> selectRecentByUserIdAndType(@Param("userId") Long userId, 
                                                     @Param("chatType") String chatType, 
                                                     @Param("limit") Integer limit);
    
    /**
     * 统计用户对话总数
     */
    @Select("SELECT COUNT(*) FROM ai_chat_history WHERE user_id = #{userId} AND is_deleted = 0")
    Long countByUserId(Long userId);
    
    /**
     * 删除对话记录（软删除）
     */
    @Update("UPDATE ai_chat_history SET is_deleted = 1, update_time = NOW(), updater = #{updater} " +
            "WHERE id = #{id}")
    int deleteById(@Param("id") Long id, @Param("updater") Long updater);
    
    /**
     * 清理指定用户的旧对话记录（保留最近N条）
     */
    @Update("UPDATE ai_chat_history SET is_deleted = 1, update_time = NOW() " +
            "WHERE user_id = #{userId} AND id NOT IN (" +
            "SELECT t.id FROM (SELECT id FROM ai_chat_history WHERE user_id = #{userId} " +
            "AND is_deleted = 0 ORDER BY create_time DESC LIMIT #{keepCount}) t)")
    int cleanOldRecords(@Param("userId") Long userId, @Param("keepCount") Integer keepCount);
}
