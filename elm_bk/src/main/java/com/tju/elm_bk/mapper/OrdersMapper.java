
// OrdersMapper.java
package com.tju.elm_bk.mapper;
import com.tju.elm_bk.dto.OrderDTO;
import com.tju.elm_bk.entity.Order;

import com.tju.elm_bk.vo.OrderItemDetailVO;
import com.tju.elm_bk.vo.OrderItemVO;
import com.tju.elm_bk.vo.OrderVO;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;
import com.tju.elm_bk.vo.CustomerStatsVO;

@Mapper
public interface OrdersMapper {
    @Select("SELECT COUNT(*) orderCount, SUM(CASE WHEN order_state=7 THEN 1 ELSE 0 END) completedCount, SUM(CASE WHEN order_state=8 THEN 1 ELSE 0 END) cancelledCount, SUM(CASE WHEN order_state=9 THEN 1 ELSE 0 END) exceptionCount, COALESCE(SUM(CASE WHEN order_state=7 THEN order_total ELSE 0 END),0) revenue FROM orders WHERE is_deleted=0 AND order_date >= #{from} AND order_date < #{to}")
    Map<String,Object> aggregateStats(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);
    @Select("SELECT id FROM orders WHERE order_state = 0 AND is_deleted = 0 AND order_date < DATE_SUB(NOW(), INTERVAL 15 MINUTE)")
    List<Long> findExpiredWaitingPaymentIds();

    @Update("UPDATE orders SET order_state = 8, updater = 0, update_time = NOW() WHERE id = #{orderId} AND order_state = 0 AND is_deleted = 0")
    int cancelExpiredWaitingPayment(@Param("orderId") Long orderId);

    @Select("SELECT id FROM orders WHERE order_state = 6 AND is_deleted = 0 AND update_time < DATE_SUB(NOW(), INTERVAL 24 HOUR)")
    List<Long> findExpiredDeliveredIds();

    @Update("UPDATE orders SET order_state = 7, updater = 0, update_time = NOW() WHERE id = #{orderId} AND order_state = 6 AND is_deleted = 0")
    int completeExpiredDelivered(@Param("orderId") Long orderId);
    List<OrderVO> selectOrders(Long userId);

    OrderVO selectOrderById(Long orderId);

    void insertOrder(Order order);



    void insertOrderPlus(Order order);

    @Select("""
        <script>
            select o.id,o.order_total,o.order_state,o.order_date,o.business_id,o.delivery_price,o.service_mode,b.business_name,b.business_img
            from orders o
            left join business b on b.id = o.business_id
            <where>
                o.is_deleted = 0
                <if test="null != businessId">
                    and o.business_id = #{businessId}
                </if>
                <if test="null != orderState">
                    and o.order_state = #{orderState}
                </if>
                <if test="null != userId">
                    and o.customer_id = #{userId}
                </if>
            </where>
              order by o.order_date desc
        </script>
    """)
    List<OrderItemVO> selectOrderItemsList(Long businessId, Integer orderState,Long userId);

    @Select("""
        <script>
            select o.*, uc.username as customerName, b.business_name,b.business_img,
                   COALESCE(o.address_snapshot, da.address) AS address,
                   COALESCE(o.contact_name_snapshot, da.contact_name) AS contact_name,
                   COALESCE(o.contact_sex_snapshot, da.contact_sex) AS contact_sex,
                   COALESCE(o.contact_tel_snapshot, da.contact_tel) AS contact_tel
            from orders o
            left join users uc on uc.id = o.customer_id
            left join business b on b.id = o.business_id
            left join delivery_address da on da.id = o.address_id
            <where>
                o.is_deleted = 0
                <if test="null != businessId">
                    and o.business_id = #{businessId}
                </if>
                <if test="null != orderState">
                    and o.order_state = #{orderState}
                </if>
            </where>
              order by o.order_date desc
        </script>
    """)
    List<OrderItemDetailVO> selectOrderDetailetItem(Long businessId, Integer orderState);

    @Select("""
        <script>
            select o.*, uc.username as customerName, b.business_name,b.business_img,
                   COALESCE(o.address_snapshot, da.address) AS address,
                   COALESCE(o.contact_name_snapshot, da.contact_name) AS contact_name,
                   COALESCE(o.contact_sex_snapshot, da.contact_sex) AS contact_sex,
                   COALESCE(o.contact_tel_snapshot, da.contact_tel) AS contact_tel
            from orders o
            left join users uc on uc.id = o.customer_id
            left join business b on b.id = o.business_id
            left join delivery_address da on da.id = o.address_id
            where o.is_deleted = 0 and o.id = #{orderItemId}
        </script>
    """)
    OrderItemDetailVO selectOrderItemById(Long orderItemId);



    @Update("update orders set order_state = #{orderState} where id = #{orderId}")
    Integer setOrderState(Long orderId, Integer orderState);

    @Update("""
        UPDATE orders
        SET order_state = #{targetState}, updater = #{operatorUserId}, update_time = NOW()
        WHERE id = #{orderId} AND order_state = #{expectedState} AND is_deleted = 0
        """)
    int updateOrderStateIfCurrent(@Param("orderId") Long orderId,
                                  @Param("expectedState") Integer expectedState,
                                  @Param("targetState") Integer targetState,
                                  @Param("operatorUserId") Long operatorUserId);

    @Update("UPDATE orders SET order_total=#{orderTotal}, payment_method=#{paymentMethod}, points_used=#{pointsUsed}, wallet_paid=#{walletPaid}, payment_status='PAID', updater=#{userId}, update_time=NOW() WHERE id=#{orderId} AND is_deleted=0")
    int updateOrderPayment(@Param("orderId") Long orderId, @Param("orderTotal") java.math.BigDecimal orderTotal,
                           @Param("paymentMethod") String paymentMethod, @Param("pointsUsed") Integer pointsUsed,
                           @Param("walletPaid") Boolean walletPaid, @Param("userId") Long userId);

    @Update("UPDATE orders SET payment_status=#{status}, update_time=NOW() WHERE id=#{orderId} AND is_deleted=0")
    int updatePaymentStatus(@Param("orderId") Long orderId, @Param("status") String status);

    @Select("select * from orders where id = #{orderId}")
    Order getOrderById(Long orderId);

    @Select("SELECT id FROM orders WHERE customer_id=#{customerId} AND idempotency_key=#{idempotencyKey} AND is_deleted=0 LIMIT 1")
    Long findIdByIdempotencyKey(@Param("customerId") Long customerId, @Param("idempotencyKey") String idempotencyKey);

    @Select("select sum(order_total) from orders where order_state = 7 and is_deleted = 0")
    Double countPrice();

    // AI服务相关查询方法
    @Select("SELECT * FROM orders WHERE id = #{id} AND is_deleted = 0")
    Order selectById(Long id);

    @Select("SELECT * FROM orders WHERE customer_id = #{userId} AND is_deleted = 0 " +
            "ORDER BY order_date DESC LIMIT #{limit}")
    List<Order> selectRecentOrdersByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    @Select("SELECT COUNT(*) completedOrderCount, COALESCE(SUM(o.order_total),0) totalSpent, COALESCE(AVG(o.order_total),0) averageOrder, COUNT(DISTINCT o.business_id) visitedBusinessCount, COUNT(DISTINCT b.order_type_id) visitedCategoryCount FROM orders o LEFT JOIN business b ON b.id=o.business_id WHERE o.customer_id=#{userId} AND o.order_state=7 AND o.is_deleted=0")
    CustomerStatsVO customerSpendingStats(@Param("userId") Long userId);
}
