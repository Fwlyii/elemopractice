package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.dto.FoodCreateDTO;
import com.tju.elm_bk.dto.FoodDTO;
import com.tju.elm_bk.dto.FoodUpdateDTO;
import com.tju.elm_bk.entity.Authority;
import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.entity.Food;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.AuthorityMapper;
import com.tju.elm_bk.mapper.BusinessMapper;
import com.tju.elm_bk.mapper.FoodMapper;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.service.FoodService;
import com.tju.elm_bk.utils.ObjectCopyUtil;
import com.tju.elm_bk.utils.SecurityUtils;
import com.tju.elm_bk.vo.FoodItemVO;
import com.tju.elm_bk.vo.FoodVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class FoodServiceImpl implements FoodService {
    @Autowired
    private FoodMapper foodMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BusinessMapper businessMapper;

    @Override
    public List<FoodVO> getFoodList(Integer business, Integer order) {
        return foodMapper.selectFoodVOList(business, order);
    }

    @Override
    public FoodVO getFoodById(Long id) {
        return foodMapper.selectFoodVOById(id);
    }

    @Override
    public FoodVO addFood(FoodDTO foodDTO) {
        if(!foodDTO.verify()) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
        User user = userMapper.findByUsernameWithAuthorities(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));
        List<Authority> authorities = user.getAuthorities();
        Business business = businessMapper.selectBusinessById(foodDTO.getBusiness().getId());
        if (business == null) {
            throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        }
        if (authorities.stream()
                .noneMatch(auth -> "ADMIN".equals(auth.getName()))
                && !Objects.equals(user.getId(), business.getUserId())) {
            throw new APIException(ResultCodeEnum.UNAUTHORIZED);
        }

        Food food = new Food();
        BeanUtils.copyProperties(foodDTO, food);
        food.setBusinessId(foodDTO.getBusiness().getId());
        food.setCreator(user.getId());
        food.setCreateTime(LocalDateTime.now());
        food.setUpdater(user.getId());
        food.setUpdateTime(LocalDateTime.now());
        food.setIsDeleted(false);
        foodMapper.insertFood(food);
        return foodMapper.selectFoodVOById(food.getId());
    }

    @Override
    public FoodVO updateFood(FoodDTO foodDTO,Long id) {
        if (foodDTO == null) {
            return null;
        }
        Food food = foodMapper.selectFoodById(id);
        if (food == null) {
            throw new APIException(ResultCodeEnum.FOOD_MISSED);
        }

        User user = userMapper.findByUsernameWithAuthorities(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));
        List<Authority> authorities = user.getAuthorities();
        Business business = businessMapper.selectBusinessById(foodDTO.getBusiness().getId());
        if (authorities.stream()
                .noneMatch(auth -> "ADMIN".equals(auth.getName()))
                && !Objects.equals(user.getId(), business.getUserId())) {
            throw new APIException(ResultCodeEnum.UNAUTHORIZED);
        }

        food.setFoodName(foodDTO.getFoodName() == null ? food.getFoodName() : foodDTO.getFoodName());
        food.setFoodExplain(foodDTO.getFoodExplain() == null ? food.getFoodExplain() : foodDTO.getFoodExplain());
        food.setFoodPrice(foodDTO.getFoodPrice() == null ? food.getFoodPrice() : foodDTO.getFoodPrice());
        food.setFoodImg(foodDTO.getFoodImg() == null ? food.getFoodImg() : foodDTO.getFoodImg());
        food.setRemarks(foodDTO.getRemarks() == null ? food.getRemarks() : foodDTO.getRemarks());
        food.setUpdater(user.getId());
        food.setUpdateTime(LocalDateTime.now());
        foodMapper.updateFood(food,id);
        return foodMapper.selectFoodVOById(food.getId());
    }


    @Override
    public List<FoodItemVO> getFoodItemList(Long businessId, Integer shelveStatus) {
        User user = userMapper.findByUsernameWithAuthorities(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));
        List<Authority> authorities = user.getAuthorities();

        // 普通用户只能看到已上架的商品
        if (authorities.stream()
                .noneMatch(authority -> (Objects.equals(authority.getName(), "BUSINESS") || Objects.equals(authority.getName(), "ADMIN")))) {
            shelveStatus = 1;
        }

        return foodMapper.selectFoodItemVOList(businessId, shelveStatus);
    }

    @Override
    public Long addFoodItem(FoodCreateDTO foodCreateDTO) {
        if (!foodCreateDTO.verify()) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
        Business business = businessMapper.selectBusinessById(foodCreateDTO.getBusinessId());
        if (business == null) {
            throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        }

        User user = userMapper.findByUsernameWithAuthorities(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));
        List<Authority> authorities = user.getAuthorities();

        // 非管理员只能添加自己商铺的商品
        if (authorities.stream()
                .noneMatch(authority -> Objects.equals(authority.getName(), "ADMIN"))
                && !Objects.equals(user.getId(), business.getUserId())) {
            throw new APIException(ResultCodeEnum.NOT_ENOUGH_PERMISSION);
        }

        Food food = new Food();
        BeanUtils.copyProperties(foodCreateDTO, food);
        food.setBusinessId(food.getBusinessId());
        food.setCreator(user.getId());
        food.setCreateTime(LocalDateTime.now());
        food.setUpdater(user.getId());
        food.setUpdateTime(LocalDateTime.now());
        food.setIsDeleted(false);
        foodMapper.insertFood(food);

        return food.getId();
    }

    @Override
    public Long setFoodStatus(Long foodId, Integer shelveStatus) {
        Food food = foodMapper.selectFoodById(foodId);
        if (food == null) {
            throw new APIException(ResultCodeEnum.FOOD_MISSED);
        }
        Business business = businessMapper.selectBusinessById(food.getBusinessId());

        User user = userMapper.findByUsernameWithAuthorities(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));
        List<Authority> authorities = user.getAuthorities();
        if (authorities.stream()
                .noneMatch(authority -> Objects.equals(authority.getName(), "ADMIN"))
                && !Objects.equals(user.getId(), business.getUserId())) {
            throw new APIException(ResultCodeEnum.NOT_ENOUGH_PERMISSION);
        }

        if (shelveStatus != 0 && shelveStatus != 1) {
            throw new APIException(ResultCodeEnum.FOOD_STATUS_SET_FAILED);
        }

        foodMapper.updateFoodStatus(foodId, shelveStatus);

        return foodId;
    }

    @Override
    @Transactional
    public Long modifyFoodMessage(FoodUpdateDTO foodUpdateDTO) {
        if (foodUpdateDTO.getFoodId() == null) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
        Food food = foodMapper.selectFoodById(foodUpdateDTO.getFoodId());
        if (food == null) {
            throw new APIException(ResultCodeEnum.FOOD_MISSED);
        }

        Business business = businessMapper.selectBusinessById(food.getBusinessId());
        User user = userMapper.findByUsernameWithAuthorities(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));
        List<Authority> authorities = user.getAuthorities();
        if (authorities.stream()
                .noneMatch(authority -> Objects.equals(authority.getName(), "ADMIN"))
                && !Objects.equals(user.getId(), business.getUserId())) {
            throw new APIException(ResultCodeEnum.NOT_ENOUGH_PERMISSION);
        }

        ObjectCopyUtil.copyPropertiesIgnoreNull(foodUpdateDTO,food);
        food.setUpdater(user.getId());
        food.setUpdateTime(LocalDateTime.now());
        foodMapper.updateFoodMessage(food);
        return food.getId();
    }

    @Override
    public Long deleteFood(Long foodId) {
        Food food = foodMapper.selectFoodById(foodId);
        if (food == null) {
            throw new APIException(ResultCodeEnum.FOOD_MISSED);
        }
        Business business = businessMapper.selectBusinessById(food.getBusinessId());
        User user = userMapper.findByUsernameWithAuthorities(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));
        List<Authority> authorities = user.getAuthorities();
        if (authorities.stream()
                .noneMatch(authority -> Objects.equals(authority.getName(), "ADMIN"))
                && !Objects.equals(user.getId(), business.getUserId())) {
            throw new APIException(ResultCodeEnum.NOT_ENOUGH_PERMISSION);
        }

        foodMapper.deleteFood(foodId);
        return foodId;
    }
}
