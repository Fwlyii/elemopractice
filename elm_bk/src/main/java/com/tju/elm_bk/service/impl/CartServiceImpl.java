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
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.service.CartService;
import com.tju.elm_bk.utils.SecurityUtils;
import com.tju.elm_bk.vo.CartItemVO;
import com.tju.elm_bk.vo.CartVO;
import com.tju.elm_bk.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BusinessMapper businessMapper;
    @Autowired
    private FoodMapper foodMapper;

    @Override
    public CartVO addCart(CartItemCreateDTO cartItemCreateDTO) {
        if (!cartItemCreateDTO.verify()) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }

        User user = userMapper.findByUsername(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));

        if(!Objects.equals(userMapper.getUserIdByUsername(cartItemCreateDTO.getCustomer().getUsername()), user.getId())) {
            throw new APIException(ResultCodeEnum.USER_UNMATCHED);
        }

        Food food = foodMapper.selectFoodById(cartItemCreateDTO.getFood().getId());
        if (food == null) {
            throw new APIException(ResultCodeEnum.FOOD_MISSED);
        }
//        Business business = businessMapper.selectBusinessById(cartItemCreateDTO.getBusiness().getId());
//        if (business == null) {
//            throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
//        }

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
        User customer = userMapper.findByUsernameWithAuthorities(userMapper.findById(cart.getCustomerId()).getUsername());
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(customer,userVO);
        cartVO.setCustomer(userVO);
        cartVO.setBusiness(businessMapper.selectBusinessVO(cartVO.getBusinessId()));
        return cartVO;
    }





    @Override
    public List<CartItemVO> getCartItemList(Long businessId) {
        Long userId = userMapper.getUserIdByUsername(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));
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
        Business business = businessMapper.selectBusinessById(food.getBusinessId());
        if (business == null) {
            throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        }

        if (quantity <= 0) {
            throw new APIException(ResultCodeEnum.QUANTITY_ILLEGAL);
        }

        Long userId = userMapper.getUserIdByUsername(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));
        Cart cart = new Cart();

        cart.setCustomerId(userId);
        cart.setFoodId(foodId);
        cart.setQuantity(quantity);
        cart.setBusinessId(business.getId());

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
        Long userId = userMapper.getUserIdByUsername(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));

        Cart cart = cartMapper.selectCartById(cartId);
        if (cart == null) {
            throw new APIException(ResultCodeEnum.CART_MISSED);
        }
        if (!Objects.equals(cart.getCustomerId(), userId)) {
            throw new APIException(ResultCodeEnum.USER_DENIED);
        }
        if (quantity < 0) {
            throw new APIException(ResultCodeEnum.QUANTITY_ILLEGAL);
        }
        if (quantity == 0) {
            cartMapper.removeCartItem(cartId);
            return cartId;
        }

        cartMapper.updateCartItem(cartId, quantity);
        return cart.getId();
    }

    @Override
    public Long clearCart(Long businessId) {
        Long userId = userMapper.getUserIdByUsername(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));
        cartMapper.clearCart(userId, businessId);
        return businessId;
    }

    @Override
    public Long removeItem(Long cartId) {
        Long userId = userMapper.getUserIdByUsername(SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED)));
        Cart cart = cartMapper.selectCartById(cartId);
        if (!Objects.equals(cart.getCustomerId(), userId)) {
            throw new APIException(ResultCodeEnum.USER_DENIED);
        }
        cartMapper.removeCartItem(cartId);
        return cartId;
    }
}
