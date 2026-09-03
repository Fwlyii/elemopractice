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
    CartVO selectCart(@Param("cartId") Long cartId);



    @Select("""
        select c.id,c.business_id,c.quantity,c.food_id,
           f.food_name,f.food_price,f.food_img,f.stock,f.category,f.purchase_limit,b.business_name
        from cart c
        join food f on f.id = c.food_id and f.is_deleted = 0
        join business b on c.business_id = b.id and b.is_deleted = 0
        where c.customer_id = #{userId} and c.is_deleted = 0 and c.business_id = #{businessId};
    """)
    List<CartItemVO> selectCartItems(@Param("userId") Long userId, @Param("businessId") Long businessId);

    @Select("select * from cart where id = #{cartId} and is_deleted = 0")
    Cart selectCartById(@Param("cartId") Long cartId);

    @Select("SELECT * FROM cart WHERE customer_id = #{userId} AND business_id = #{businessId} AND food_id = #{foodId} AND is_deleted = 0 LIMIT 1")
    Cart selectActiveCartItem(@Param("userId") Long userId, @Param("businessId") Long businessId,
                              @Param("foodId") Long foodId);

    @Update("update cart set quantity = #{quantity} where id = #{cartId}")
    void updateCartItem(@Param("cartId") Long cartId, @Param("quantity") Integer quantity);

    @Update("update cart set is_deleted = 1 where customer_id = #{userId} and business_id = #{businessId}")
    void clearCart(@Param("userId") Long userId, @Param("businessId") Long businessId);

    @Update({"<script>",
            "update cart set is_deleted = 1 where customer_id = #{userId} and business_id = #{businessId} and food_id in",
            "<foreach collection='foodIds' item='foodId' open='(' separator=',' close=')'>#{foodId}</foreach>",
            "</script>"})
    void clearCartItems(@Param("userId") Long userId, @Param("businessId") Long businessId,
                        @Param("foodIds") List<Long> foodIds);

    @Update("update cart set is_deleted = 1 where id = #{cartId}")
    void removeCartItem(@Param("cartId") Long cartId);


}
