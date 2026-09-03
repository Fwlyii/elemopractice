package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.entity.UserAsset;
import com.tju.elm_bk.entity.UserCoupon;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.AssetMapper;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.service.AssetService;
import com.tju.elm_bk.utils.SecurityUtils;
import com.tju.elm_bk.vo.AssetVO;
import com.tju.elm_bk.vo.AssetLedgerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {
    private final AssetMapper assetMapper;
    private final UserMapper userMapper;
    private User current() {
        String username=SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException("请先登录"));
        User user=userMapper.findByUsername(username); if(user==null) throw new APIException("当前用户不存在"); return user;
    }
    private AssetVO snapshot(Long userId) {
        assetMapper.ensure(userId); var a=assetMapper.findByUserId(userId);
        LocalDateTime expire=a.getMembershipExpire();
        return new AssetVO(a.getBalance(),a.getPoints(),expire,expire!=null&&expire.isAfter(LocalDateTime.now()),assetMapper.countCoupons(userId));
    }
    @Override public AssetVO me(){ return snapshot(current().getId()); }
    @Override @Transactional public AssetVO recharge(BigDecimal amount){
        if(amount==null||amount.compareTo(BigDecimal.ONE)<0||amount.compareTo(new BigDecimal("500"))>0) throw new APIException("充值金额需在1-500元之间");
        Long id=current().getId(); assetMapper.ensure(id); assetMapper.addBalance(id,amount); assetMapper.insertLedger(id,"RECHARGE",amount,0,"模拟充值",null); return snapshot(id);
    }
    @Override @Transactional public AssetVO claimWelcomeCoupon(){ Long id=current().getId(); assetMapper.ensure(id); if (assetMapper.claimWelcomeCoupon(id) == 1) assetMapper.insertLedger(id,"COUPON_GRANT",BigDecimal.ZERO,0,"领取新人券",null); return snapshot(id); }
    @Override public List<UserCoupon> availableCoupons(){ return assetMapper.listAvailableCoupons(current().getId()); }
    @Override @Transactional public AssetVO activateMembership(){
        Long id=current().getId(); assetMapper.ensure(id); UserAsset current=assetMapper.findByUserId(id);
        if (current.getMembershipExpire() != null && current.getMembershipExpire().isAfter(LocalDateTime.now())) return snapshot(id);
        assetMapper.activateMembership(id); assetMapper.insertLedger(id,"MEMBERSHIP",BigDecimal.ZERO,0,"开通30天会员",null); return snapshot(id);
    }
    @Override public List<AssetLedgerVO> ledger(Integer limit){ Long id=current().getId(); int safeLimit=limit==null?20:Math.min(Math.max(limit,1),100); return assetMapper.listLedger(id,safeLimit); }

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
