package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.dto.FoodCreateDTO;
import com.tju.elm_bk.dto.FoodDTO;
import com.tju.elm_bk.dto.FoodUpdateDTO;
import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.entity.Food;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.BusinessMapper;
import com.tju.elm_bk.mapper.FoodMapper;
import com.tju.elm_bk.mapper.OrdersMapper;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.service.CurrentUserService;
import com.tju.elm_bk.service.FoodService;
import com.tju.elm_bk.utils.ObjectCopyUtil;
import com.tju.elm_bk.vo.FoodItemVO;
import com.tju.elm_bk.vo.FoodVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FoodServiceImpl implements FoodService {
    private final FoodMapper foodMapper;
    private final BusinessMapper businessMapper;
    private final OrdersMapper ordersMapper;
    private final CurrentUserService currentUserService;

    @Override
    public List<FoodVO> getFoodList(Integer business, Integer order) {
        User user = currentUser();
        boolean privileged = isAdmin(user);
        if (order != null) {
            com.tju.elm_bk.entity.Order targetOrder = ordersMapper.getOrderById(order.longValue());
            if (targetOrder == null) throw new APIException(ResultCodeEnum.ORDER_MISSED);
            Business orderBusiness = businessMapper.selectBusinessById(targetOrder.getBusinessId());
            privileged = privileged || Objects.equals(targetOrder.getCustomerId(), user.getId())
                    || (orderBusiness != null && Objects.equals(orderBusiness.getUserId(), user.getId()));
            if (!privileged) throw new APIException(ResultCodeEnum.USER_DENIED);
        } else if (business != null) {
            Business targetBusiness = businessMapper.selectBusinessById(business.longValue());
            if (targetBusiness == null) throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
            privileged = privileged || Objects.equals(targetBusiness.getUserId(), user.getId());
        }
        return foodMapper.selectFoodVOList(business, order, !privileged);
    }

    @Override
    public FoodVO getFoodById(Long id) {
        Food food = foodMapper.selectFoodById(id);
        if (food == null) throw new APIException(ResultCodeEnum.FOOD_MISSED);
        Business business = businessMapper.selectBusinessById(food.getBusinessId());
        User user = currentUser();
        if (!isAdmin(user) && (business == null || !Objects.equals(business.getUserId(), user.getId()))
                && (food.getShelveStatus() == null || food.getShelveStatus() != 1
                || business.getStatus() == null || business.getStatus() != 1)) {
            throw new APIException(ResultCodeEnum.FOOD_MISSED);
        }
        return foodMapper.selectFoodVOById(id);
    }

    @Override
    public FoodVO addFood(FoodDTO foodDTO) {
        if(!foodDTO.verify()) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
        Business business = businessMapper.selectBusinessById(foodDTO.getBusiness().getId());
        if (business == null) {
            throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        }
        User user = requireBusinessManager(business, ResultCodeEnum.UNAUTHORIZED);

        Food food = new Food();
        BeanUtils.copyProperties(foodDTO, food);
        food.setBusinessId(foodDTO.getBusiness().getId());
        food.setCreator(user.getId());
        food.setCreateTime(LocalDateTime.now());
        food.setUpdater(user.getId());
        food.setUpdateTime(LocalDateTime.now());
        food.setIsDeleted(false);
        food.setStock(foodDTO.getStock() == null ? 100 : foodDTO.getStock());
        food.setCategory(normalizeCategory(foodDTO.getCategory()));
        food.setPurchaseLimit(normalizePurchaseLimit(foodDTO.getPurchaseLimit()));
        foodMapper.insertFood(food);
        return foodMapper.selectFoodVOById(food.getId());
    }

    @Override
    public FoodVO updateFood(FoodDTO foodDTO,Long id) {
        if (foodDTO == null) {
            return null;
        }
        validateFoodUpdate(foodDTO);
        Food food = foodMapper.selectFoodById(id);
        if (food == null) {
            throw new APIException(ResultCodeEnum.FOOD_MISSED);
        }

        if (foodDTO.getBusiness() == null || foodDTO.getBusiness().getId() == null
                || !Objects.equals(food.getBusinessId(), foodDTO.getBusiness().getId())) {
            throw new APIException("商品所属商家与请求参数不一致");
        }

        Business business = businessMapper.selectBusinessById(food.getBusinessId());
        if (business == null) {
            throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        }
        User user = requireBusinessManager(business, ResultCodeEnum.UNAUTHORIZED);

        food.setFoodName(foodDTO.getFoodName() == null ? food.getFoodName() : foodDTO.getFoodName());
        food.setFoodExplain(foodDTO.getFoodExplain() == null ? food.getFoodExplain() : foodDTO.getFoodExplain());
        food.setFoodPrice(foodDTO.getFoodPrice() == null ? food.getFoodPrice() : foodDTO.getFoodPrice());
        food.setFoodImg(foodDTO.getFoodImg() == null ? food.getFoodImg() : foodDTO.getFoodImg());
        food.setRemarks(foodDTO.getRemarks() == null ? food.getRemarks() : foodDTO.getRemarks());
        food.setCategory(foodDTO.getCategory() == null ? food.getCategory() : normalizeCategory(foodDTO.getCategory()));
        food.setPurchaseLimit(foodDTO.getPurchaseLimit() == null ? food.getPurchaseLimit() : normalizePurchaseLimit(foodDTO.getPurchaseLimit()));
        food.setUpdater(user.getId());
        food.setUpdateTime(LocalDateTime.now());
        foodMapper.updateFood(food,id);
        return foodMapper.selectFoodVOById(food.getId());
    }


    @Override
    public List<FoodItemVO> getFoodItemList(Long businessId, Integer shelveStatus) {
        Business business = businessMapper.selectBusinessById(businessId);
        if (business == null) throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        User user = currentUserService.optionalUser().orElse(null);
        boolean admin = user != null && isAdmin(user);
        boolean owner = user != null && Objects.equals(user.getId(), business.getUserId());

        // 下架商品属于商家后台数据：只有本店商家或管理员可以查询。
        if (!admin && !owner) {
            if (!Objects.equals(business.getStatus(), 1)) throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
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

        User user = requireBusinessManager(business, ResultCodeEnum.NOT_ENOUGH_PERMISSION);

        Food food = new Food();
        BeanUtils.copyProperties(foodCreateDTO, food);
        food.setBusinessId(food.getBusinessId());
        food.setCreator(user.getId());
        food.setCreateTime(LocalDateTime.now());
        food.setUpdater(user.getId());
        food.setUpdateTime(LocalDateTime.now());
        food.setIsDeleted(false);
        food.setStock(foodCreateDTO.getStock() == null ? 100 : foodCreateDTO.getStock());
        food.setCategory(normalizeCategory(foodCreateDTO.getCategory()));
        food.setPurchaseLimit(normalizePurchaseLimit(foodCreateDTO.getPurchaseLimit()));
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
        if (business == null) throw new APIException(ResultCodeEnum.BUSINESS_MISSED);

        requireBusinessManager(business, ResultCodeEnum.NOT_ENOUGH_PERMISSION);

        if (shelveStatus != 0 && shelveStatus != 1) {
            throw new APIException(ResultCodeEnum.FOOD_STATUS_SET_FAILED);
        }

        foodMapper.updateFoodStatus(foodId, shelveStatus);

        return foodId;
    }

    @Override
    @Transactional
    public Long modifyFoodMessage(FoodUpdateDTO foodUpdateDTO) {
        if (foodUpdateDTO == null || foodUpdateDTO.getFoodId() == null) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
        Food food = foodMapper.selectFoodById(foodUpdateDTO.getFoodId());
        if (food == null) {
            throw new APIException(ResultCodeEnum.FOOD_MISSED);
        }

        Business business = businessMapper.selectBusinessById(food.getBusinessId());
        if (business == null) throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        User user = requireBusinessManager(business, ResultCodeEnum.NOT_ENOUGH_PERMISSION);

        ObjectCopyUtil.copyPropertiesIgnoreNull(foodUpdateDTO,food);
        validateFoodEntity(food);
        if (foodUpdateDTO.getCategory() != null) food.setCategory(normalizeCategory(foodUpdateDTO.getCategory()));
        if (foodUpdateDTO.getPurchaseLimit() != null) food.setPurchaseLimit(normalizePurchaseLimit(foodUpdateDTO.getPurchaseLimit()));
        food.setUpdater(user.getId());
        food.setUpdateTime(LocalDateTime.now());
        foodMapper.updateFoodMessage(food);
        return food.getId();
    }

    @Override
    @Transactional
    public Long updateStock(FoodUpdateDTO foodUpdateDTO) {
        if (foodUpdateDTO == null || foodUpdateDTO.getFoodId() == null || foodUpdateDTO.getStock() == null
                || foodUpdateDTO.getStock() < 0 || foodUpdateDTO.getStock() > 1_000_000) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
        Food food = foodMapper.selectFoodById(foodUpdateDTO.getFoodId());
        if (food == null) throw new APIException(ResultCodeEnum.FOOD_MISSED);
        Business business = businessMapper.selectBusinessById(food.getBusinessId());
        if (business == null) throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        User user = requireBusinessManager(business, ResultCodeEnum.NOT_ENOUGH_PERMISSION);
        foodUpdateDTO.setFoodPrice(food.getFoodPrice());
        foodUpdateDTO.setFoodName(food.getFoodName());
        foodUpdateDTO.setFoodExplain(food.getFoodExplain());
        foodUpdateDTO.setFoodImg(food.getFoodImg());
        foodUpdateDTO.setRemarks(food.getRemarks());
        food.setStock(foodUpdateDTO.getStock());
        if (food.getCategory() == null || food.getCategory().isBlank()) food.setCategory("其他");
        food.setUpdater(user.getId()); food.setUpdateTime(LocalDateTime.now());
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
        if (business == null) throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        requireBusinessManager(business, ResultCodeEnum.NOT_ENOUGH_PERMISSION);

        foodMapper.deleteFood(foodId);
        return foodId;
    }

    private String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) return "其他";
        String normalized = category.trim();
        return normalized.length() > 32 ? normalized.substring(0, 32) : normalized;
    }

    private Integer normalizePurchaseLimit(Integer purchaseLimit) {
        if (purchaseLimit == null) return null;
        if (purchaseLimit <= 0) throw new APIException("单笔限购数量必须大于0");
        return Math.min(purchaseLimit, 999);
    }

    private User currentUser() {
        return currentUserService.requireUser();
    }

    private boolean isAdmin(User user) {
        return currentUserService.isAdmin(user);
    }

    private User requireBusinessManager(Business business, ResultCodeEnum deniedCode) {
        User user = currentUser();
        if (!isAdmin(user) && !Objects.equals(user.getId(), business.getUserId())) {
            throw new APIException(deniedCode);
        }
        return user;
    }

    private void validateFoodUpdate(FoodDTO dto) {
        if (dto.getFoodName() != null && (dto.getFoodName().trim().isEmpty() || dto.getFoodName().trim().length() > 100)) {
            throw new APIException("商品名称不能为空");
        }
        if (dto.getFoodPrice() != null && (dto.getFoodPrice().compareTo(java.math.BigDecimal.ZERO) <= 0
                || dto.getFoodPrice().compareTo(new java.math.BigDecimal("100000")) > 0)) {
            throw new APIException("商品价格必须大于0且不超过100000元");
        }
        if (dto.getStock() != null && (dto.getStock() < 0 || dto.getStock() > 1_000_000)) {
            throw new APIException("库存必须在0到1000000之间");
        }
        if (dto.getPurchaseLimit() != null && dto.getPurchaseLimit() <= 0) {
            throw new APIException("单笔限购数量必须大于0");
        }
    }

    private void validateFoodEntity(Food food) {
        if (food.getFoodName() == null || food.getFoodName().trim().isEmpty() || food.getFoodName().trim().length() > 100
                || food.getFoodPrice() == null || food.getFoodPrice().compareTo(java.math.BigDecimal.ZERO) <= 0
                || food.getFoodPrice().compareTo(new java.math.BigDecimal("100000")) > 0
                || (food.getStock() != null && (food.getStock() < 0 || food.getStock() > 1_000_000))) {
            throw new APIException("商品信息不合法");
        }
    }
}
