package com.tju.elm_bk.controller;

import com.tju.elm_bk.dto.CartItemCreateDTO;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.service.CartService;
import com.tju.elm_bk.vo.CartItemVO;
import com.tju.elm_bk.vo.CartVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carts")
@Tag(name="管理购物车")
public class CartController {
    @Autowired
    private CartService cartService;

    @PostMapping
    @Operation(summary = "向购物车添加商品",description = "老师测试用")
    public HttpResult<CartVO> addCartItem(@RequestBody CartItemCreateDTO cartItemCreateDTO) {
        return HttpResult.success(cartService.addCart(cartItemCreateDTO));
    }




    @GetMapping("/list")
    @Operation(summary = "获取用户在指定商家的购物车商品列表")
    public HttpResult<List<CartItemVO>> addCartItem(@RequestParam Long businessId) {
        return HttpResult.success(cartService.getCartItemList(businessId));
    }

    @GetMapping("/add")
    @Operation(summary = "(前端用这个)向购物车添加商品")
    public HttpResult<Long> addCartItem(@RequestParam Long foodId, @RequestParam Integer quantity) {
        return HttpResult.success(cartService.addItem(foodId, quantity));
    }

    @GetMapping("/quantity")
    @Operation(summary = "修改购物车指定商品数量",description = "quantity传0时移除该条记录")
    public HttpResult<Long> updateItemQuantity(@RequestParam Long cartId, @RequestParam Integer quantity) {
        return HttpResult.success(cartService.updateItem(cartId,quantity));
    }

    @GetMapping("/clear")
    @Operation(summary = "清空用户在指定商家的购物车")
    public HttpResult<Long> updateItemQuantity(@RequestParam Long businessId) {
        return HttpResult.success(cartService.clearCart(businessId));
    }

    @GetMapping("/remove")
    @Operation(summary = "移除指定购物车商品")
    public HttpResult<Long> removeItem(@RequestParam Long cartId) {
        return HttpResult.success(cartService.removeItem(cartId));
    }


}
