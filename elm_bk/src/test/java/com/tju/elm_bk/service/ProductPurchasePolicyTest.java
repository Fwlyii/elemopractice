package com.tju.elm_bk.service;

import com.tju.elm_bk.entity.Food;
import com.tju.elm_bk.exception.APIException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductPurchasePolicyTest {
    private final ProductPurchasePolicy policy = new ProductPurchasePolicy();

    @Test
    void allowsNormalQuantityWhenNoPurchaseLimitIsConfigured() {
        Food food = food("普通菜品", 100, null, 1);
        food.setStock(2_000);
        assertDoesNotThrow(() -> policy.validateCartQuantity(food, 999));
        APIException error = assertThrows(APIException.class,
                () -> policy.validateCartQuantity(food, 1_000));
        assertEquals("单件商品每单最多购买999份", error.getMessage());
    }

    @Test
    void explainsPurchaseLimitInsteadOfReturningGenericFailure() {
        Food food = food("限量套餐", 100, 2, 1);
        APIException error = assertThrows(APIException.class,
                () -> policy.validateCartQuantity(food, 3));
        assertEquals("商品“限量套餐”每单最多购买2份", error.getMessage());
    }

    @Test
    void explainsAvailableStockAndRejectsUnshelvedProducts() {
        Food lowStock = food("今日甜品", 2, null, 1);
        APIException stockError = assertThrows(APIException.class,
                () -> policy.validateCartQuantity(lowStock, 3));
        assertEquals("商品“今日甜品”库存不足，当前最多可购买2份", stockError.getMessage());

        Food unshelved = food("下架商品", 100, null, 0);
        APIException shelfError = assertThrows(APIException.class,
                () -> policy.validateCartQuantity(unshelved, 1));
        assertEquals("商品已下架", shelfError.getMessage());
    }

    @Test
    void reportsTheSmallestEffectiveLimitWhenStockAndPurchaseLimitBothApply() {
        Food purchaseLimitFirst = food("小份甜点", 20, 2, 1);
        APIException purchaseLimitError = assertThrows(APIException.class,
                () -> policy.validateCartQuantity(purchaseLimitFirst, 3));
        assertEquals("商品“小份甜点”每单最多购买2份", purchaseLimitError.getMessage());

        Food stockFirst = food("每日鲜食", 2, 5, 1);
        APIException stockError = assertThrows(APIException.class,
                () -> policy.validateCartQuantity(stockFirst, 3));
        assertEquals("商品“每日鲜食”库存不足，当前最多可购买2份", stockError.getMessage());
    }

    private Food food(String name, Integer stock, Integer limit, Integer shelveStatus) {
        Food food = new Food();
        food.setFoodName(name);
        food.setStock(stock);
        food.setPurchaseLimit(limit);
        food.setShelveStatus(shelveStatus);
        return food;
    }
}
