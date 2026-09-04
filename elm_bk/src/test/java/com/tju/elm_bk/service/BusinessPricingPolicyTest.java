package com.tju.elm_bk.service;

import com.tju.elm_bk.exception.APIException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessPricingPolicyTest {

    private final BusinessPricingPolicy policy = new BusinessPricingPolicy();

    @Test
    void acceptsNoPromotionOrOneCompleteValidPromotion() {
        assertDoesNotThrow(() -> policy.validate(money("0"), money("0"), null, null));
        assertDoesNotThrow(() -> policy.validate(money("20"), money("3"), money("30"), money("5")));
    }

    @Test
    void rejectsPartialOrSelfConsumingPromotionConfiguration() {
        assertThrows(APIException.class,
                () -> policy.validate(money("20"), money("3"), money("30"), null));
        assertThrows(APIException.class,
                () -> policy.validate(money("20"), money("3"), money("30"), money("30")));
        assertThrows(APIException.class,
                () -> policy.validate(money("20"), money("3"), money("0"), money("0")));
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
