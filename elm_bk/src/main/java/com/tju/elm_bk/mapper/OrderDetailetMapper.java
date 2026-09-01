// OrderDetailetMapper.java
package com.tju.elm_bk.mapper;

import com.tju.elm_bk.entity.OrderDetailet;

import com.tju.elm_bk.vo.OrderFoodVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderDetailetMapper {

    Integer saveOrderDetail(OrderDetailet orderDetailet);




    Integer saveOrderDetailPlus(OrderDetailet orderDetailet);

    @Select("""
        select od.id,od.quantity,od.food_id,
           f.food_name,od.food_price,
           o.id as order_id
        from orderdetailet od
        left join food f on f.id = od.food_id
        left join orders o on o.id = od.order_id
        where od.order_id = #{orderId}
    """)
    List<OrderFoodVO> selectOrderDetailList(Long orderId);
}