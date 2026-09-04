package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.dto.CartItemCreateDTO;
import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.entity.Cart;
import com.tju.elm_bk.entity.Food;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.BusinessMapper;
import com.tju.elm_bk.mapper.CartMapper;
import com.tju.elm_bk.mapper.FoodMapper;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.service.CartService;
import com.tju.elm_bk.service.CurrentUserService;
import com.tju.elm_bk.service.ProductPurchasePolicy;
import com.tju.elm_bk.vo.CartItemVO;
import com.tju.elm_bk.vo.CartVO;
import com.tju.elm_bk.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartMapper cartMapper;
    private final BusinessMapper businessMapper;
    private final FoodMapper foodMapper;
    private final CurrentUserService currentUserService;
    private final ProductPurchasePolicy productPurchasePolicy;

    @Override
    public CartVO addCart(CartItemCreateDTO cartItemCreateDTO) {
        if (!cartItemCreateDTO.verify()) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }

        User user = currentUserService.requireUser();

        if(!Objects.equals(cartItemCreateDTO.getCustomer().getUsername(), user.getUsername())) {
            throw new APIException(ResultCodeEnum.USER_UNMATCHED);
        }

        Food food = foodMapper.selectFoodById(cartItemCreateDTO.getFood().getId());
        if (food == null) {
            throw new APIException(ResultCodeEnum.FOOD_MISSED);
        }
        Cart existing = cartMapper.selectActiveCartItem(user.getId(), food.getBusinessId(), food.getId());
        long desiredQuantity = (long) (existing == null || existing.getQuantity() == null ? 0 : existing.getQuantity())
                + cartItemCreateDTO.getQuantity();
        ensurePurchasable(food, desiredQuantity);
        if (existing != null) {
            cartMapper.updateCartItem(existing.getId(), Math.toIntExact(desiredQuantity));
            CartVO merged = cartMapper.selectCart(existing.getId());
            merged.setCustomer(toUserVO(user));
            merged.setBusiness(businessMapper.selectBusinessVO(existing.getBusinessId()));
            return merged;
        }

        Cart cart = new Cart();
        cart.setQuantity(cartItemCreateDTO.getQuantity());
        cart.setBusinessId(food.getBusinessId());
        cart.setFoodId(cartItemCreateDTO.getFood().getId());
        cart.setCustomerId(user.getId());
        cart.setCreator(user.getId());
        cart.setUpdater(user.getId());
        cart.setCreateTime(LocalDateTime.now());
        cart.setUpdateTime(LocalDateTime.now());
        cart.setIsDeleted(false);

        cartMapper.insertCart(cart);
        CartVO cartVO = cartMapper.selectCart(cart.getId());
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);
        cartVO.setCustomer(userVO);
        cartVO.setBusiness(businessMapper.selectBusinessVO(cartVO.getBusinessId()));
        return cartVO;
    }





    @Override
    public List<CartItemVO> getCartItemList(Long businessId) {
        Long userId = currentUserService.requireUserId();
        return cartMapper.selectCartItems(userId, businessId);
    }

    @Override
    public Long addItem(Long foodId, Integer quantity) {
        Food food = foodMapper.selectFoodById(foodId);
        if (food == null) {
            throw new APIException(ResultCodeEnum.FOOD_MISSED);
        }
        if (food.getShelveStatus() != 1) {
            throw new APIException(ResultCodeEnum.FOOD_UNSHELVED);
        }
        if (quantity == null || quantity <= 0) {
            throw new APIException(ResultCodeEnum.QUANTITY_ILLEGAL);
        }

        Long userId = currentUserService.requireUserId();
        Cart existing = cartMapper.selectActiveCartItem(userId, food.getBusinessId(), foodId);
        long desiredQuantity = (long) (existing == null || existing.getQuantity() == null ? 0 : existing.getQuantity()) + quantity;
        ensurePurchasable(food, desiredQuantity);
        if (existing != null) {
            cartMapper.updateCartItem(existing.getId(), Math.toIntExact(desiredQuantity));
            return existing.getId();
        }
        Cart cart = new Cart();

        cart.setCustomerId(userId);
        cart.setFoodId(foodId);
        cart.setQuantity(quantity);
        cart.setBusinessId(food.getBusinessId());

        cart.setCreator(userId);
        cart.setUpdater(userId);
        cart.setCreateTime(LocalDateTime.now());
        cart.setUpdateTime(LocalDateTime.now());
        cart.setIsDeleted(false);

        cartMapper.insertCart(cart);

        return cart.getId();
    }

    @Override
    public Long updateItem(Long cartId, Integer quantity) {
        Long userId = currentUserService.requireUserId();

        Cart cart = cartMapper.selectCartById(cartId);
        if (cart == null) {
            throw new APIException(ResultCodeEnum.CART_MISSED);
        }
        if (!Objects.equals(cart.getCustomerId(), userId)) {
            throw new APIException(ResultCodeEnum.USER_DENIED);
        }
        if (quantity == null || quantity < 0) {
            throw new APIException(ResultCodeEnum.QUANTITY_ILLEGAL);
        }
        if (quantity == 0) {
            cartMapper.removeCartItem(cartId);
            return cartId;
        }

        Food food = foodMapper.selectFoodById(cart.getFoodId());
        if (food == null) throw new APIException("商品已下架或不存在");
        ensurePurchasable(food, quantity.longValue());

        cartMapper.updateCartItem(cartId, quantity);
        return cart.getId();
    }

    @Override
    public Long clearCart(Long businessId) {
        Long userId = currentUserService.requireUserId();
        cartMapper.clearCart(userId, businessId);
        return businessId;
    }

    @Override
    public Long removeItem(Long cartId) {
        Long userId = currentUserService.requireUserId();
        Cart cart = cartMapper.selectCartById(cartId);
        if (cart == null) {
            throw new APIException(ResultCodeEnum.CART_MISSED);
        }
        if (!Objects.equals(cart.getCustomerId(), userId)) {
            throw new APIException(ResultCodeEnum.USER_DENIED);
        }
        cartMapper.removeCartItem(cartId);
        return cartId;
    }

    private void ensurePurchasable(Food food, Long desiredQuantity) {
        if (desiredQuantity == null) throw new APIException(ResultCodeEnum.QUANTITY_ILLEGAL);
        productPurchasePolicy.validateCartQuantity(food, desiredQuantity);
        Business business = businessMapper.selectBusinessById(food.getBusinessId());
        if (business == null) throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        if (business.getStatus() != null && business.getStatus() != 1) {
            throw new APIException("商家当前未营业");
        }
        if (Boolean.FALSE.equals(business.getOperatingStatus())) {
            throw new APIException("商家当前休息中");
        }
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
