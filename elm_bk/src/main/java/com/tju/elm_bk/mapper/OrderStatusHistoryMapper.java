package com.tju.elm_bk.mapper;

import com.tju.elm_bk.entity.OrderStatusHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderStatusHistoryMapper {
    @Insert("""
        INSERT INTO order_status_history
          (order_id, from_status, to_status, operator_user_id, reason, create_time)
        VALUES
          (#{orderId}, #{fromStatus}, #{toStatus}, #{operatorUserId}, #{reason}, NOW())
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(OrderStatusHistory history);

    @Select("SELECT * FROM order_status_history WHERE order_id = #{orderId} ORDER BY create_time, id")
    List<OrderStatusHistory> listByOrderId(Long orderId);
}
