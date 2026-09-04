package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.entity.UserAsset;
import com.tju.elm_bk.entity.UserCoupon;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.AssetMapper;
import com.tju.elm_bk.service.AssetService;
import com.tju.elm_bk.service.CurrentUserService;
import com.tju.elm_bk.vo.AssetVO;
import com.tju.elm_bk.vo.AssetLedgerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {
    private final AssetMapper assetMapper;
    private final CurrentUserService currentUserService;
    @Value("${app.demo.enabled:false}")
    private boolean demoEnabled;

    private User current() {
        return currentUserService.requireUser();
    }

    private AssetVO snapshot(Long userId) {
        assetMapper.ensure(userId);
        UserAsset asset = assetMapper.findByUserId(userId);
        LocalDateTime expire = asset.getMembershipExpire();
        return new AssetVO(asset.getBalance(), asset.getPoints(), expire,
                expire != null && expire.isAfter(LocalDateTime.now()),
                assetMapper.countCoupons(userId), assetMapper.countWelcomeCoupons(userId) > 0);
    }

    @Override
    public AssetVO me() {
        return snapshot(current().getId());
    }

    @Override
    @Transactional
    public AssetVO recharge(BigDecimal amount) {
        requireDemoFeature("模拟充值");
        if (amount == null || amount.compareTo(BigDecimal.ONE) < 0
                || amount.compareTo(new BigDecimal("500")) > 0) {
            throw new APIException("充值金额需在1-500元之间");
        }
        Long userId = current().getId();
        assetMapper.ensure(userId);
        UserAsset asset = assetMapper.lockByUserId(userId);
        BigDecimal currentBalance = asset.getBalance() == null ? BigDecimal.ZERO : asset.getBalance();
        if (currentBalance.add(amount).compareTo(new BigDecimal("2000")) > 0) {
            throw new APIException("演示钱包余额不能超过2000元");
        }
        assetMapper.addBalance(userId, amount);
        assetMapper.insertLedger(userId, "RECHARGE", amount, 0, "模拟充值", null);
        return snapshot(userId);
    }

    @Override
    @Transactional
    public AssetVO claimWelcomeCoupon() {
        Long userId = current().getId();
        assetMapper.ensure(userId);
        // 锁住该用户唯一的资产行，使“检查+领取”在并发请求下仍只成功一次。
        assetMapper.lockByUserId(userId);
        if (assetMapper.countWelcomeCoupons(userId) > 0) throw new APIException("新人券每个账号仅可领取一次");
        if (assetMapper.claimWelcomeCoupon(userId) != 1) throw new APIException("新人券领取失败，请勿重复操作");
        assetMapper.insertLedger(userId, "COUPON_GRANT", BigDecimal.ZERO, 0, "领取新人券", null);
        return snapshot(userId);
    }

    @Override
    public List<UserCoupon> availableCoupons() {
        return assetMapper.listAvailableCoupons(current().getId());
    }

    @Override
    @Transactional
    public AssetVO activateMembership() {
        requireDemoFeature("免费开通会员");
        Long userId = current().getId();
        assetMapper.ensure(userId);
        UserAsset asset = assetMapper.lockByUserId(userId);
        if (asset.getMembershipExpire() != null && asset.getMembershipExpire().isAfter(LocalDateTime.now())) {
            return snapshot(userId);
        }
        assetMapper.activateMembership(userId);
        assetMapper.insertLedger(userId, "MEMBERSHIP", BigDecimal.ZERO, 0, "开通30天会员", null);
        return snapshot(userId);
    }

    @Override
    public List<AssetLedgerVO> ledger(Integer limit) {
        Long userId = current().getId();
        int safeLimit = limit == null ? 20 : Math.min(Math.max(limit, 1), 100);
        return assetMapper.listLedger(userId, safeLimit);
    }

    private void requireDemoFeature(String feature) {
        if (!demoEnabled) throw new APIException(feature + "仅在课程演示环境开放");
    }

    @Override @Transactional
    public void refundOrderAssets(Long orderId, Long userId, Integer pointsUsed, BigDecimal walletAmount) {
        if (userId == null) return;
        assetMapper.ensure(userId);
        if (walletAmount != null && walletAmount.compareTo(BigDecimal.ZERO) > 0) {
            assetMapper.addBalance(userId, walletAmount);
            assetMapper.insertLedger(userId, "WALLET_REFUND", walletAmount, 0, "订单取消退回钱包余额", orderId);
        }
        if (pointsUsed != null && pointsUsed > 0) {
            assetMapper.addPoints(userId, pointsUsed);
            assetMapper.insertLedger(userId, "POINT_REFUND", BigDecimal.ZERO, pointsUsed, "订单取消退回抵扣积分", orderId);
        }
    }
}
