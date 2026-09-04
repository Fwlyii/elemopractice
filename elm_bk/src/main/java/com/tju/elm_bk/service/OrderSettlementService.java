package com.tju.elm_bk.service;

import com.tju.elm_bk.entity.Order;
import com.tju.elm_bk.entity.UserCoupon;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.AssetMapper;
import com.tju.elm_bk.mapper.FoodMapper;
import com.tju.elm_bk.mapper.OrdersMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 订单资金和用户资产的唯一结算入口。
 * 订单、配送和超时任务只组织流程，不再各写一套扣款/退款逻辑。
 */
@Service
@RequiredArgsConstructor
public class OrderSettlementService {
    private static final BigDecimal POINTS_PER_YUAN = new BigDecimal("100");
    private static final BigDecimal MAX_POINT_DISCOUNT_RATE = new BigDecimal("0.20");

    private final AssetMapper assetMapper;
    private final OrdersMapper ordersMapper;
    private final FoodMapper foodMapper;
    private final AssetService assetService;

    @Value("${app.demo.enabled:false}")
    private boolean demoEnabled;

    /** 支付必须在待支付 -> 待商家接单的同一事务中调用。 */
    @Transactional
    public void settlePayment(Order order, Long userId, String requestedMethod,
                              Integer requestedPoints, Long couponId) {
        String method = normalizePaymentMethod(requestedMethod);
        boolean wallet = "wallet".equals(method);
        int points = normalizePoints(requestedPoints);
        BigDecimal orderAmount = zero(order.getOrderTotal());

        UserCoupon coupon = validateCoupon(couponId, userId, order, orderAmount);
        BigDecimal couponDiscount = couponDiscount(coupon, order, orderAmount);
        BigDecimal afterCoupon = orderAmount.subtract(couponDiscount).max(BigDecimal.ZERO);
        validatePointLimit(points, afterCoupon);

        assetMapper.ensure(userId);
        deductPoints(order, userId, points);
        BigDecimal pointDiscount = BigDecimal.valueOf(points)
                .divide(POINTS_PER_YUAN, 2, RoundingMode.DOWN);
        BigDecimal payable = afterCoupon.subtract(pointDiscount)
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        deductWallet(order, userId, wallet, payable);
        consumeCoupon(coupon, order.getId());

        String storedMethod = wallet ? "WALLET" : "SIMULATED";
        if (ordersMapper.updateOrderPayment(order.getId(), payable, storedMethod, points, wallet, userId) != 1) {
            throw new APIException("支付记录保存失败，请重试");
        }
        order.setOrderTotal(payable);
        order.setPointsUsed(points);
        order.setWalletPaid(wallet);
        order.setPaymentStatus("PAID");
    }

    /** 取消订单的库存、红包、积分和钱包回滚使用同一实现。 */
    @Transactional
    public void cancelOrder(Order order, boolean paid) {
        foodMapper.restoreStockByOrder(order.getId());
        assetMapper.releaseCouponByOrder(order.getId());
        ordersMapper.updatePaymentStatus(order.getId(), paid ? "REFUNDED" : "CANCELLED");
        if (Boolean.TRUE.equals(order.getWalletPaid()) || positive(order.getPointsUsed())) {
            assetService.refundOrderAssets(order.getId(), order.getCustomerId(), order.getPointsUsed(),
                    Boolean.TRUE.equals(order.getWalletPaid()) ? zero(order.getOrderTotal()) : BigDecimal.ZERO);
        }
    }

    @Transactional
    public void awardCompletionPoints(Order order, Long customerId) {
        assetMapper.ensure(customerId);
        int points = zero(order.getOrderTotal()).setScale(0, RoundingMode.FLOOR).intValue();
        if (points <= 0) return;
        assetMapper.addPoints(customerId, points);
        assetMapper.insertLedger(customerId, "POINT_EARN", BigDecimal.ZERO, points,
                "完成订单奖励积分", order.getId());
    }

    private String normalizePaymentMethod(String requestedMethod) {
        String method = requestedMethod == null ? "" : requestedMethod.trim().toLowerCase();
        if (method.isEmpty()) {
            if (!demoEnabled) throw new APIException("请选择有效的支付方式");
            return "simulated";
        }
        if ("alipay".equals(method) || "wechat".equals(method)) {
            throw new APIException("支付宝和微信支付尚未接入服务端支付回调，不能直接标记为已支付");
        }
        if ("simulated".equals(method) && !demoEnabled) {
            throw new APIException("模拟支付仅在课程演示环境开放");
        }
        if (!"wallet".equals(method) && !"simulated".equals(method)) {
            throw new APIException("不支持的支付方式");
        }
        return method;
    }

    private int normalizePoints(Integer requestedPoints) {
        int points = requestedPoints == null ? 0 : requestedPoints;
        if (points < 0 || points % 100 != 0) {
            throw new APIException("积分抵扣必须为100的整数倍");
        }
        return points;
    }

    private UserCoupon validateCoupon(Long couponId, Long userId, Order order, BigDecimal orderAmount) {
        if (couponId == null) return null;
        UserCoupon coupon = assetMapper.findCouponForUser(couponId, userId);
        if (coupon == null) throw new APIException("红包不可用或已过期，请刷新结算页");
        BigDecimal base = couponBase(order, orderAmount);
        if (coupon.getMinOrderAmount() != null && base.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new APIException("当前订单未达到该红包使用门槛");
        }
        return coupon;
    }

    private BigDecimal couponDiscount(UserCoupon coupon, Order order, BigDecimal orderAmount) {
        if (coupon == null) return BigDecimal.ZERO;
        BigDecimal base = couponBase(order, orderAmount);
        return zero(coupon.getDiscountAmount()).max(BigDecimal.ZERO).min(base);
    }

    private BigDecimal couponBase(Order order, BigDecimal orderAmount) {
        return orderAmount.subtract(zero(order.getDeliveryPrice())).max(BigDecimal.ZERO);
    }

    private void validatePointLimit(int points, BigDecimal afterCoupon) {
        int maxPoints = afterCoupon.multiply(MAX_POINT_DISCOUNT_RATE).multiply(POINTS_PER_YUAN)
                .setScale(0, RoundingMode.FLOOR).intValue();
        if (points > maxPoints) throw new APIException("本单积分最多抵扣应付金额的20%");
    }

    private void deductPoints(Order order, Long userId, int points) {
        if (points <= 0) return;
        if (assetMapper.subtractPointsIfEnough(userId, points) != 1) throw new APIException("积分余额不足");
        assetMapper.insertLedger(userId, "POINT_REDEEM", BigDecimal.ZERO, -points,
                "订单支付积分抵扣", order.getId());
    }

    private void deductWallet(Order order, Long userId, boolean wallet, BigDecimal payable) {
        if (!wallet) return;
        if (assetMapper.subtractBalanceIfEnough(userId, payable) != 1) {
            throw new APIException("钱包余额不足，请选择其他支付方式或先充值");
        }
        if (payable.signum() > 0) {
            assetMapper.insertLedger(userId, "WALLET_PAY", payable.negate(), 0,
                    "订单钱包支付", order.getId());
        }
    }

    private void consumeCoupon(UserCoupon coupon, Long orderId) {
        if (coupon != null && assetMapper.useCoupon(coupon.getId(), orderId) != 1) {
            throw new APIException("红包已被使用，请刷新结算页");
        }
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean positive(Integer value) {
        return value != null && value > 0;
    }
}
