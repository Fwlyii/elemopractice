package com.tju.elm_bk.mapper;

import com.tju.elm_bk.entity.RiderProfile;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface RiderMapper {

    @Select("""
        SELECT rp.*, u.username
        FROM rider_profile rp
        JOIN users u ON u.id = rp.user_id
        WHERE rp.user_id = #{userId}
        """)
    RiderProfile findByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT rp.*, u.username
        FROM rider_profile rp
        JOIN users u ON u.id = rp.user_id
        WHERE rp.id = #{id}
        """)
    RiderProfile findById(@Param("id") Long id);

    @Select("""
        <script>
        SELECT rp.*, u.username
        FROM rider_profile rp
        JOIN users u ON u.id = rp.user_id
        <where>
          <if test="auditStatus != null">rp.audit_status = #{auditStatus}</if>
        </where>
        ORDER BY rp.create_time DESC
        </script>
        """)
    List<RiderProfile> listApplications(@Param("auditStatus") Integer auditStatus);

    @Insert("""
        INSERT INTO rider_profile
          (user_id, real_name, phone, vehicle_type, audit_status, online,
           completed_orders, total_distance, total_income, create_time, update_time)
        VALUES
          (#{userId}, #{realName}, #{phone}, #{vehicleType}, #{auditStatus}, 0,
           0, 0, 0, NOW(), NOW())
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(RiderProfile profile);

    @Update("""
        UPDATE rider_profile
        SET real_name = #{realName}, phone = #{phone}, vehicle_type = #{vehicleType},
            audit_status = 0, online = 0, reject_reason = NULL, update_time = NOW()
        WHERE user_id = #{userId} AND audit_status = 2
        """)
    int resubmit(RiderProfile profile);

    @Update("""
        UPDATE rider_profile
        SET audit_status = #{auditStatus}, reject_reason = #{reason}, online = 0, update_time = NOW()
        WHERE id = #{id} AND audit_status = 0
        """)
    int audit(@Param("id") Long id, @Param("auditStatus") Integer auditStatus, @Param("reason") String reason);

    @Update("""
        UPDATE rider_profile SET online = #{online}, update_time = NOW()
        WHERE user_id = #{userId} AND audit_status = 1
        """)
    int updateOnline(@Param("userId") Long userId, @Param("online") Boolean online);

    @Update("""
        UPDATE rider_profile
        SET completed_orders = completed_orders + 1,
            total_distance = total_distance + #{distance},
            total_income = total_income + #{income},
            update_time = NOW()
        WHERE user_id = #{userId}
        """)
    int addCompletedStats(@Param("userId") Long userId,
                          @Param("distance") BigDecimal distance,
                          @Param("income") BigDecimal income);
}
