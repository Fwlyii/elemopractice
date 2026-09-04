package com.tju.elm_bk.service;

import com.tju.elm_bk.constant.PurchaseRules;
import com.tju.elm_bk.entity.Food;
import com.tju.elm_bk.exception.APIException;
import org.springframework.stereotype.Component;

/**
 * 商品购买数量的统一规则入口。
 *
 * 购物车和正式下单都必须复用这里的数量、上架、库存与限购语义，避免不同页面
 * 对同一商品给出互相矛盾的结果。库存仍由下单时的原子 SQL 做最终并发保护。
 */
@Component
public class ProductPurchasePolicy {
    public void validateCartQuantity(Food food, long desiredQuantity) {
        validateQuantityBounds(desiredQuantity);
        if (food.getShelveStatus() == null || food.getShelveStatus() != 1) {
            throw new APIException("商品已下架");
        }
        Integer stock = food.getStock();
        Integer purchaseLimit = food.getPurchaseLimit();
        // 库存和商家限购同时命中时，提示真正先达到的那条边界，与顾客端保持一致。
        if (purchaseLimit != null && (stock == null || purchaseLimit <= stock)) {
            validatePurchaseLimit(food.getFoodName(), purchaseLimit, desiredQuantity);
        }
        if (stock != null && desiredQuantity > stock) {
            throw new APIException("商品“" + safeName(food.getFoodName()) + "”库存不足，当前最多可购买"
                    + Math.max(0, stock) + "份");
        }
        validatePurchaseLimit(food.getFoodName(), purchaseLimit, desiredQuantity);
    }

    public void validateQuantityBounds(long desiredQuantity) {
        if (desiredQuantity <= 0 || desiredQuantity > PurchaseRules.MAX_QUANTITY_PER_ITEM) {
            throw new APIException("单件商品每单最多购买" + PurchaseRules.MAX_QUANTITY_PER_ITEM + "份");
        }
    }

    public void validatePurchaseLimit(String foodName, Integer purchaseLimit, long desiredQuantity) {
        if (purchaseLimit != null && desiredQuantity > purchaseLimit) {
            throw new APIException("商品“" + safeName(foodName) + "”每单最多购买" + purchaseLimit + "份");
        }
    }

    private String safeName(String foodName) {
        return foodName == null ? "" : foodName;
    }
}
