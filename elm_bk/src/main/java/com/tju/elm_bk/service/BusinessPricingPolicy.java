package com.tju.elm_bk.service;

import com.tju.elm_bk.exception.APIException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 店铺起送价、配送费与满减配置的单一校验规则。 */
@Component
public class BusinessPricingPolicy {
    private static final BigDecimal MAX_START_PRICE = new BigDecimal("100000");
    private static final BigDecimal MAX_DELIVERY_PRICE = new BigDecimal("10000");
    private static final BigDecimal MAX_PROMOTION_AMOUNT = new BigDecimal("100000");

    public void validate(BigDecimal startPrice, BigDecimal deliveryPrice,
                         BigDecimal promotionThreshold, BigDecimal promotionDiscount) {
        BigDecimal start = zeroIfNull(startPrice);
        BigDecimal delivery = zeroIfNull(deliveryPrice);
        if (start.signum() < 0 || start.compareTo(MAX_START_PRICE) > 0
                || delivery.signum() < 0 || delivery.compareTo(MAX_DELIVERY_PRICE) > 0) {
            throw new APIException("起送价和配送费必须为合理的非负数字");
        }
        if (promotionThreshold == null && promotionDiscount == null) return;

        if (promotionThreshold == null || promotionDiscount == null
                || promotionThreshold.compareTo(BigDecimal.ONE) < 0
                || promotionThreshold.compareTo(MAX_PROMOTION_AMOUNT) > 0
                || promotionDiscount.signum() <= 0
                || promotionDiscount.compareTo(MAX_PROMOTION_AMOUNT) > 0
                || promotionDiscount.compareTo(promotionThreshold) >= 0) {
            throw new APIException("满减优惠配置不合法：优惠金额必须小于门槛");
        }
    }

    public boolean hasValidPromotion(BigDecimal threshold, BigDecimal discount) {
        return threshold != null && discount != null
                && threshold.compareTo(BigDecimal.ONE) >= 0
                && discount.signum() > 0
                && discount.compareTo(threshold) < 0;
    }

    public BigDecimal merchantDiscount(BigDecimal subtotal, BigDecimal threshold, BigDecimal discount) {
        if (!hasValidPromotion(threshold, discount) || subtotal == null
                || subtotal.compareTo(threshold) < 0 || discount.compareTo(subtotal) >= 0) {
            return BigDecimal.ZERO;
        }
        return discount;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
