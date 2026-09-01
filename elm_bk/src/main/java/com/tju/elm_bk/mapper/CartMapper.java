package com.tju.elm_bk.mapper;

import com.tju.elm_bk.entity.Cart;
import com.tju.elm_bk.vo.CartItemVO;
import com.tju.elm_bk.vo.CartVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CartMapper {

    void insertCart(Cart cart);

    @Select("SELECT * FROM cart C WHERE C.id = #{cartId} AND C.is_deleted = 0")
    CartVO selectCart(Long cartId);



    @Select("""
        select c.id,c.business_id,c.quantity,c.food_id,
           f.food_name,f.food_price,f.food_img,b.business_name
        from cart c
        left join food f on f.id = c.food_id and f.is_deleted = 0
        left join business b on c.business_id = b.id and b.is_deleted = 0
        where c.customer_id = #{userId} and c.is_deleted = 0 and c.business_id = #{businessId};
    """)
    List<CartItemVO> selectCartItems(Long userId,Long businessId);

    @Select("select * from cart where id = #{cartId} and is_deleted = 0")
    Cart selectCartById(Long cartId);

    @Update("update cart set quantity = #{quantity} where id = #{cartId}")
    void updateCartItem(Long cartId,Integer quantity);

    @Update("update cart set is_deleted = 1 where customer_id = #{userId} and business_id = #{businessId}")
    void clearCart(Long userId,Long businessId);

    @Update("update cart set is_deleted = 1 where id = #{cartId}")
    void removeCartItem(Long cartId);


}
