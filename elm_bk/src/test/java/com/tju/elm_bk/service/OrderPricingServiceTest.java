package com.tju.elm_bk.service;

import com.tju.elm_bk.constant.FulfillmentMode;
import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.entity.UserAsset;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.vo.CartItemVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderPricingServiceTest {

    private final OrderPricingService service = new OrderPricingService(new BusinessPricingPolicy());

    @Test
    void serverQuoteCombinesMerchantPromotionMembershipAndDelivery() {
        Business business = business();
        UserAsset member = new UserAsset();
        member.setMembershipExpire(LocalDateTime.now().plusDays(1));

        OrderPricingService.OrderQuote quote = service.quote(
                business, List.of(item("20.00", 2)), member, FulfillmentMode.DELIVERY);

        assertEquals(new BigDecimal("40.00"), quote.subtotal());
        assertEquals(new BigDecimal("5.00"), quote.merchantDiscount());
        assertEquals(new BigDecimal("1.75"), quote.membershipDiscount());
        assertEquals(new BigDecimal("3.00"), quote.deliveryFee());
        assertEquals(new BigDecimal("36.25"), quote.total());
    }

    @Test
    void pickupNeverChargesDeliveryOrAppliesDeliveryStartPrice() {
        Business business = business();
        business.setStartPrice(new BigDecimal("100"));

        OrderPricingService.OrderQuote quote = service.quote(
                business, List.of(item("10.00", 1)), null, FulfillmentMode.PICKUP);

        assertEquals(new BigDecimal("0.00"), quote.deliveryFee());
        assertEquals(new BigDecimal("10.00"), quote.total());
    }

    @Test
    void deliveryStillEnforcesStartPriceOnServer() {
        Business business = business();
        business.setStartPrice(new BigDecimal("50"));

        assertThrows(APIException.class, () -> service.quote(
                business, List.of(item("10.00", 1)), null, FulfillmentMode.DELIVERY));
    }

    private Business business() {
        Business business = new Business();
        business.setStatus(1);
        business.setOperatingStatus(true);
        business.setDineInAvailable(true);
        business.setStartPrice(new BigDecimal("20"));
        business.setDeliveryPrice(new BigDecimal("3"));
        business.setPromotionThreshold(new BigDecimal("30"));
        business.setPromotionDiscount(new BigDecimal("5"));
        return business;
    }

    private CartItemVO item(String price, int quantity) {
        CartItemVO item = new CartItemVO();
        item.setFoodId(1L);
        item.setFoodName("\u6d4b\u8bd5\u5546\u54c1");
        item.setFoodPrice(new BigDecimal(price));
        item.setQuantity(quantity);
        return item;
    }
}
