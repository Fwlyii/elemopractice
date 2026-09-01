package com.tju.elm_bk.service;

import com.tju.elm_bk.dto.FoodCreateDTO;
import com.tju.elm_bk.dto.FoodDTO;
import com.tju.elm_bk.dto.FoodUpdateDTO;
import com.tju.elm_bk.vo.FoodItemVO;
import com.tju.elm_bk.vo.FoodVO;

import java.util.List;

public interface FoodService {

    List<FoodVO> getFoodList(Integer business,Integer order);

    FoodVO getFoodById(Long id);

    FoodVO addFood(FoodDTO food);

    FoodVO updateFood(FoodDTO foodDTO,Long id);


    List<FoodItemVO> getFoodItemList(Long businessId,Integer shelveStatus);

    Long addFoodItem(FoodCreateDTO foodCreateDTO);

    Long setFoodStatus(Long foodId,Integer shelveStatus);

    Long modifyFoodMessage(FoodUpdateDTO foodUpdateDTO);

    Long deleteFood(Long foodId);


}
