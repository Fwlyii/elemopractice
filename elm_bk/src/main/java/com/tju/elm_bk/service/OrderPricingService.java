package com.tju.elm_bk.service;

import com.tju.elm_bk.constant.FulfillmentMode;
import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.entity.UserAsset;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.vo.CartItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务端订单试算。前端只展示结果，商品价格、会员、满减和配送费不接受客户端报价。
 */
@Service
@RequiredArgsConstructor
public class OrderPricingService {
    private static final BigDecimal MAX_ORDER_AMOUNT = new BigDecimal("99999999.99");
    private static final int MAX_ITEM_QUANTITY = 999;
    private static final BigDecimal MEMBER_RATE = new BigDecimal("0.95");

    private final BusinessPricingPolicy businessPricingPolicy;

    public OrderQuote quote(Business business, List<CartItemVO> items, UserAsset assets, FulfillmentMode mode) {
        if (business == null || items == null || items.isEmpty() || mode == null) {
            throw new APIException(ResultCodeEnum.ORDER_SUBMIT_FAILED);
        }
        BigDecimal subtotal = calculateSubtotal(items);
        if (mode == FulfillmentMode.DELIVERY) validateStartPrice(business, subtotal);

        BigDecimal merchantDiscount = businessPricingPolicy.merchantDiscount(subtotal,
                business.getPromotionThreshold(), business.getPromotionDiscount());
        BigDecimal afterMerchantPromotion = subtotal.subtract(merchantDiscount).max(BigDecimal.ZERO);
        boolean member = assets != null && assets.getMembershipExpire() != null
                && assets.getMembershipExpire().isAfter(LocalDateTime.now());
        BigDecimal afterMembership = member
                ? afterMerchantPromotion.multiply(MEMBER_RATE)
                : afterMerchantPromotion;
        BigDecimal membershipDiscount = afterMerchantPromotion.subtract(afterMembership);
        BigDecimal deliveryFee = mode.chargesDeliveryFee()
                ? zeroIfNull(business.getDeliveryPrice()) : BigDecimal.ZERO;
        BigDecimal total = afterMembership.add(deliveryFee).max(BigDecimal.ZERO);
        if (total.compareTo(MAX_ORDER_AMOUNT) > 0) {
            throw new APIException("订单金额超过系统允许的单笔上限");
        }
        return new OrderQuote(money(subtotal), money(merchantDiscount), money(membershipDiscount),
                money(deliveryFee), money(total));
    }

    private BigDecimal calculateSubtotal(List<CartItemVO> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItemVO item : items) {
            if (item == null || item.getFoodPrice() == null || item.getFoodPrice().signum() <= 0
                    || item.getQuantity() == null || item.getQuantity() <= 0
                    || item.getQuantity() > MAX_ITEM_QUANTITY) {
                throw new APIException(ResultCodeEnum.ORDER_SUBMIT_FAILED);
            }
            subtotal = subtotal.add(item.getFoodPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            if (subtotal.compareTo(MAX_ORDER_AMOUNT) > 0) {
                throw new APIException("订单金额超过系统允许的单笔上限");
            }
        }
        if (subtotal.signum() <= 0) throw new APIException(ResultCodeEnum.ORDER_SUBMIT_FAILED);
        return subtotal;
    }

    private void validateStartPrice(Business business, BigDecimal subtotal) {
        BigDecimal startPrice = zeroIfNull(business.getStartPrice());
        if (startPrice.signum() < 0) throw new APIException("商家起送价配置异常");
        if (subtotal.compareTo(startPrice) < 0) {
            throw new APIException("订单未达到商家起送价 ¥" + money(startPrice));
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return zeroIfNull(value).setScale(2, RoundingMode.HALF_UP);
    }

    public record OrderQuote(BigDecimal subtotal, BigDecimal merchantDiscount,
                             BigDecimal membershipDiscount, BigDecimal deliveryFee, BigDecimal total) { }
}
