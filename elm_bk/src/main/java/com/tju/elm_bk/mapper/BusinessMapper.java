package com.tju.elm_bk.mapper;
import java.util.List;
import java.util.Map;

import com.tju.elm_bk.dto.BusinessDTO;
import com.tju.elm_bk.dto.BusinessInfoDTO;
import com.tju.elm_bk.dto.BusinessPermissionDTO;
import com.tju.elm_bk.dto.BusinessUpdateDTO;
import com.tju.elm_bk.entity.Authority;
import com.tju.elm_bk.vo.*;
import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.vo.BusinessVO;
import org.apache.ibatis.annotations.*;


@Mapper
public interface BusinessMapper {

    BusinessVO getBusinessById(Long businessId);

    @Select("SELECT * FROM business WHERE id = #{id}")
    BusinessPermissionVO getBusinessPermissionById(Long businessId);

    // 更新商户信息
    int updateBusiness(@Param("id") Long id, @Param("updateDto") BusinessUpdateDTO updateDto);
    int patchBusiness(@Param("id") Long id, @Param("updateDto") BusinessUpdateDTO updateDto);
    // 更新商户所有者信息
    int updateBusinessOwner(@Param("id") Long id, @Param("updateDto") BusinessUpdateDTO updateDto);
    int patchBusinessOwner(@Param("id") Long id, @Param("updateDto") BusinessUpdateDTO updateDto);
    @Update("UPDATE business SET user_id = #{ownerId} WHERE id = #{id}")
    void updateUserIdById(@Param("ownerId") Long ownerId, @Param("id") Long id);
    // 嵌套查询方法
    List<Authority> selectAuthoritiesByUserId(@Param("userId") Long userId);

    // 逻辑删除商户并返回删除前的信息
    @Update("UPDATE business SET is_deleted = 1 WHERE id = #{id} AND is_deleted = 0")
    int deleteBusiness(@Param("id") Long id);

    int insertBusiness(@Param("businessDto") BusinessDTO businessDto);

    List<BusinessVO> getBusinesses();

    @Select("SELECT b.* FROM business b WHERE b.id = #{businessId)}")
    BusinessVO selectBusinessVO(Long businessId);

    @Select("SELECT b.* FROM business b WHERE b.id = #{businessId)}")
    Business selectBusinessById(Long businessId);

    @Update("UPDATE business SET status = #{status},update_time=#{updateTime},updater=#{updater} WHERE id = #{id}")
    void updateBusinessStatus(BusinessPermissionDTO businessPermissionDTO);

    void insertBusinessPermission(BusinessPermissionDTO businessPermissionDTO);

    @Select("SELECT COUNT(*) FROM business WHERE is_deleted = 0 AND status = 1")
    Integer count();

    @Select("SELECT * FROM business WHERE status = 0 AND is_deleted = 0")
    List<BusinessPermissionVO> listNotAudited();

//    下面两个用于搜索+筛选
    List<BusinessSearchVO> searchBusinesses(@Param("keyword") String keyword);
    @MapKey("id")// 指定返回的Map的key为id
    Map<String, Object> getInteractionCounts(@Param("businessId") Long businessId);

    Integer applyForAddBusiness(Business  business);

    // 统计已完成的订单数量（ order_state=3 ）
    @Select("SELECT COUNT(*) FROM `orders` WHERE business_id = #{businessId} AND order_state = 3 AND is_deleted = 0")
    Integer getSalesCount(@Param("businessId") Long businessId);

    /**
     * 根据用户ID查询对应的所有商铺ID
     * @param userId 用户ID
     * @return 商家ID列表
     */
    @Select("SELECT id FROM business WHERE user_id = #{userId}")
    List<Long> getBusinessIdsByUserId(Long userId);

    /**
     * 获取所有已激活的商家信息
     * @return 商家信息列表
     */
    @Select("SELECT " +
            "    p.id AS userId, " +  // 注意这里改为userId，与DTO字段名一致
            "    u.username, " +
            "    p.phone, " +
            "    p.photo " +
            "FROM " +
            "    user_authority ua " +
            "    JOIN users u ON ua.user_id = u.id " +
            "    JOIN person p ON ua.user_id = p.id " +
            "WHERE " +
            "    ua.authority_name = 'BUSINESS' " +
            "    AND u.is_deleted = 0 ")
    List<BusinessInfoDTO>getAllActiveBusinesses();

    @Select("<script>" +
            "SELECT id, business_name, business_address, business_img, order_type_id, delivery_price, start_price, remarks, business_explain, status " +
            "FROM business " +
            "WHERE is_deleted = 0 " +
            "<if test='userId != null'>" +
            "   AND user_id = #{userId}" +
            "</if>" +
            "<if test='status != null'>" +
            "   AND status = #{status}" +
            "</if>" +
            "</script>")
    List<Business> selectByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);


    @Select("<script>" +
            "SELECT id, business_name, business_address, business_img, order_type_id, delivery_price, start_price, remarks, business_explain " +
            "FROM business " +
            "WHERE is_deleted = 0 AND status = 1" +
            "<if test='type != null'>" +
            "   AND order_type_id = #{type}" +
            "</if>" +
            "</script>")
    List<Business> listBusinessByOrderTypeId(@Param("type") Integer type);

    @Select("select id as merchantId, business_name as merchantName from business where user_id = #{userId} and is_deleted = 0 and status = 1")
    List<MerchantStatsVO> selectBusinessIdListByUserId(Long userId);

    // AI服务相关查询方法
    @Select("SELECT * FROM business WHERE id = #{id} AND is_deleted = 0")
    Business selectById(Long id);

    @Select("<script>" +
            "SELECT * FROM business " +
            "WHERE is_deleted = 0 AND status = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "   AND (business_name LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR business_address LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR business_explain LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "ORDER BY create_time DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<Business> searchByKeyword(@Param("keyword") String keyword, @Param("limit") Integer limit);

    @Select("SELECT id FROM business WHERE user_id = #{userId} AND is_deleted = 0 AND status = 1")
    List<Long> getBusinessIdsByUserIds(@Param("userId") Long userId);

}