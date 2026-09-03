package com.tju.elm_bk.mapper;
import java.util.List;

import com.tju.elm_bk.dto.FoodCreateDTO;
import com.tju.elm_bk.entity.Food;
import com.tju.elm_bk.vo.FoodItemVO;
import com.tju.elm_bk.vo.FoodVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FoodMapper {


    List<FoodVO> selectFoodVOList(@Param("businessId") Integer businessId, @Param("orderId") Integer orderId);

    FoodVO selectFoodVOById(@Param("id") Long id);


    void insertFood(Food food);

    @Select("SELECT * FROM food WHERE is_deleted = 0 AND id = #{id}")
    Food selectFoodById(@Param("id") Long id);

    @Update("update food set update_time = #{food.updateTime}, updater = #{food.updater}, food_explain = #{food.foodExplain}, food_img =#{food.foodImg}, food_name = #{food.foodName}, food_price = #{food.foodPrice}, remarks = #{food.remarks}, category = #{food.category}, purchase_limit = #{food.purchaseLimit} where id = #{foodId}")
    void updateFood(@Param("food") Food food, @Param("foodId") Long foodId);



    List<FoodItemVO> selectFoodItemVOList(@Param("businessId") Long businessId, @Param("shelveStatus") Integer shelveStatus);

    @Update("update food set shelve_status = #{shelveStatus} where id = #{foodId}")
    void updateFoodStatus(@Param("foodId") Long foodId, @Param("shelveStatus") Integer shelveStatus);

    /** 原子扣减库存，只有上架且库存足够时才会成功。 */
    @Update("UPDATE food SET stock = stock - #{quantity}, update_time = NOW() WHERE id = #{foodId} AND is_deleted = 0 AND shelve_status = 1 AND stock >= #{quantity}")
    int decrementStock(@Param("foodId") Long foodId, @Param("quantity") Integer quantity);

    /** 订单取消/超时后一次性恢复该订单预占的库存。 */
    @Update("UPDATE food f JOIN (SELECT food_id, SUM(quantity) quantity FROM orderdetailet WHERE order_id = #{orderId} AND is_deleted = 0 GROUP BY food_id) d ON d.food_id = f.id SET f.stock = f.stock + d.quantity, f.update_time = NOW()")
    int restoreStockByOrder(@Param("orderId") Long orderId);

    @Update("update food set update_time = #{updateTime}, updater = #{updater}, food_explain = #{foodExplain}, food_img = #{foodImg}, food_name = #{foodName}, food_price = #{foodPrice}, remarks = #{remarks}, stock = #{stock}, category = #{category}, purchase_limit = #{purchaseLimit} where id = #{id}")
    void updateFoodMessage(Food food);

    @Update("update food set is_deleted = 1 where id = #{foodId}")
    void deleteFood(@Param("foodId") Long foodId);

    // AI服务相关查询方法
    @Select("<script>" +
            "SELECT * FROM food " +
            "WHERE is_deleted = 0 AND shelve_status = 1 AND stock > 0 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "   AND (food_name LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR food_explain LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "ORDER BY create_time DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<Food> searchByKeyword(@Param("keyword") String keyword, @Param("limit") Integer limit);

}
