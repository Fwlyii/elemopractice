package com.tju.elm_bk.service;

import com.tju.elm_bk.vo.BusinessSearchVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 首页推荐的纯业务规则。新增标签、修改门槛或排序时只改这一处。
 */
@Component
@RequiredArgsConstructor
public class BusinessRecommendationPolicy {
    private static final int MAX_VISIBLE_TAGS = 3;
    private static final int NEW_BUSINESS_DAYS = 30;
    private static final BigDecimal GOOD_REVIEW_SCORE = new BigDecimal("4.7");
    private static final int GOOD_REVIEW_MIN_SALES = 200;
    private static final int LOVED_MIN_SALES = 500;
    private static final int POPULAR_MIN_SALES = 800;

    private final BusinessPricingPolicy pricingPolicy;

    public void enrich(BusinessSearchVO business, boolean recentlyPurchased) {
        int sales = business.getSalesCount() == null ? 0 : business.getSalesCount();
        BigDecimal score = business.getScore() == null ? BigDecimal.ZERO : business.getScore();
        List<RecommendationTag> candidates = new ArrayList<>();

        add(candidates, recentlyPurchased, "上次买过", 120);
        boolean validPromotion = pricingPolicy.hasValidPromotion(
                business.getPromotionThreshold(), business.getPromotionDiscount());
        add(candidates, validPromotion,
                "满" + moneyText(business.getPromotionThreshold()) + "减" + moneyText(business.getPromotionDiscount()), 105);

        boolean newBusiness = isNewBusiness(business.getCreateTime());
        add(candidates, newBusiness, "新店开业", 95);

        boolean loved = score.compareTo(GOOD_REVIEW_SCORE) >= 0 && sales >= LOVED_MIN_SALES;
        if (loved) {
            candidates.add(new RecommendationTag(formatCount(sales) + "人爱不释手", 88));
        } else {
            add(candidates, score.compareTo(GOOD_REVIEW_SCORE) >= 0 && sales >= GOOD_REVIEW_MIN_SALES,
                    "好评如潮", 85);
        }
        add(candidates, !loved && sales >= POPULAR_MIN_SALES, formatCount(sales) + "人购买", 75);
        add(candidates, Boolean.TRUE.equals(business.getDineInAvailable()), "堂食店", 65);
        add(candidates, business.getDeliveryPrice() == null || business.getDeliveryPrice().signum() == 0,
                "免配送费", 55);
        add(candidates, business.getStartPrice() != null
                && business.getStartPrice().compareTo(new BigDecimal("20")) <= 0, "低价起送", 35);

        candidates.sort(Comparator.comparingInt(RecommendationTag::priority).reversed());
        business.setRecommendationTags(candidates.stream().limit(MAX_VISIBLE_TAGS)
                .map(RecommendationTag::label).toList());

        double recommendation = (recentlyPurchased ? 120 : 0)
                + (newBusiness ? 28 : 0)
                + (validPromotion ? Math.min(24, business.getPromotionDiscount().doubleValue() * 3) : 0)
                + (score.doubleValue() - 3) * 16
                + Math.min(sales, 200) * 0.08
                + (Boolean.TRUE.equals(business.getDineInAvailable()) ? 5 : 0)
                + ((business.getDeliveryPrice() == null || business.getDeliveryPrice().signum() == 0) ? 3 : 0)
                - (Boolean.FALSE.equals(business.getOperatingStatus()) ? 1000 : 0);
        business.setRecommendationScore(BigDecimal.valueOf(Math.max(0, recommendation))
                .setScale(2, RoundingMode.HALF_UP));
    }

    private boolean isNewBusiness(LocalDateTime createTime) {
        if (createTime == null) return false;
        long age = ChronoUnit.DAYS.between(createTime, LocalDateTime.now());
        return age >= 0 && age <= NEW_BUSINESS_DAYS;
    }

    private void add(List<RecommendationTag> tags, boolean condition, String label, int priority) {
        if (condition) tags.add(new RecommendationTag(label, priority));
    }

    private String moneyText(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private String formatCount(int count) {
        return count >= 1000 ? String.format("%.1fk", count / 1000.0) : String.valueOf(count);
    }

    private record RecommendationTag(String label, int priority) { }
}
