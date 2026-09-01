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


    List<FoodVO> selectFoodVOList(Integer businessId,Integer orderId);

    FoodVO selectFoodVOById(Long id);


    void insertFood(Food food);

    @Select("SELECT * FROM food WHERE is_deleted = 0 AND id = #{id}")
    Food selectFoodById(Long id);

    @Update("update food set update_time = #{updateTime}, updater = #{updater}, foodExplain = #{foodExplain}, foodImg =#{food_img}, foodName = #{food_name}, foodPrice = #{foodPrice}, remarks = #{remarks} where id = #{foodId}")
    void updateFood(Food food,Long foodId);



    List<FoodItemVO> selectFoodItemVOList(Long businessId,Integer shelveStatus);

    @Update("update food set shelve_status = #{shelveStatus} where id = #{foodId}")
    void updateFoodStatus(Long foodId,Integer shelveStatus);

    @Update("update food set update_time = #{updateTime}, updater = #{updater}, food_explain = #{foodExplain}, food_img = #{foodImg}, food_name = #{foodName}, food_price = #{foodPrice}, remarks = #{remarks} where id = #{id}")
    void updateFoodMessage(Food food);

    @Update("update food set is_deleted = 1 where id = #{foodId}")
    void deleteFood(Long foodId);

    // AI服务相关查询方法
    @Select("<script>" +
            "SELECT * FROM food " +
            "WHERE is_deleted = 0 AND shelve_status = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "   AND (food_name LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR food_explain LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "ORDER BY create_time DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<Food> searchByKeyword(@Param("keyword") String keyword, @Param("limit") Integer limit);

}