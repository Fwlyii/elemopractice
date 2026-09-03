package com.tju.elm_bk.mapper;

import com.tju.elm_bk.entity.UserAsset;
import com.tju.elm_bk.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.tju.elm_bk.vo.AssetLedgerVO;
import java.util.List;

@Mapper
public interface AssetMapper {
    UserAsset findByUserId(@Param("userId") Long userId);
    void ensure(@Param("userId") Long userId);
    int addBalance(@Param("userId") Long userId, @Param("amount") java.math.BigDecimal amount);
    int subtractBalanceIfEnough(@Param("userId") Long userId, @Param("amount") java.math.BigDecimal amount);
    int addPoints(@Param("userId") Long userId, @Param("points") Integer points);
    int subtractPointsIfEnough(@Param("userId") Long userId, @Param("points") Integer points);
    int countCoupons(@Param("userId") Long userId);
    List<UserCoupon> listAvailableCoupons(@Param("userId") Long userId);
    UserCoupon findCouponForUser(@Param("couponId") Long couponId, @Param("userId") Long userId);
    int claimWelcomeCoupon(@Param("userId") Long userId);
    int activateMembership(@Param("userId") Long userId);
    Long findBestCouponId(@Param("userId") Long userId, @Param("subtotal") java.math.BigDecimal subtotal);
    java.math.BigDecimal findCouponDiscount(@Param("couponId") Long couponId);
    int useCoupon(@Param("couponId") Long couponId, @Param("orderId") Long orderId);
    int releaseCouponByOrder(@Param("orderId") Long orderId);
    int insertLedger(@Param("userId") Long userId, @Param("type") String type, @Param("amount") java.math.BigDecimal amount,
                     @Param("pointsDelta") Integer pointsDelta, @Param("reason") String reason, @Param("referenceId") Long referenceId);
    List<AssetLedgerVO> listLedger(@Param("userId") Long userId, @Param("limit") Integer limit);
}
