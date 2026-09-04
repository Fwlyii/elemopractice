package com.tju.elm_bk.controller;

import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.service.AssetService;
import com.tju.elm_bk.service.CurrentUserService;
import com.tju.elm_bk.vo.AssetVO;
import com.tju.elm_bk.entity.UserCoupon;
import com.tju.elm_bk.vo.CustomerStatsVO;
import com.tju.elm_bk.vo.AssetLedgerVO;
import java.util.List;
import com.tju.elm_bk.mapper.OrdersMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {
    private final AssetService assetService;
    private final OrdersMapper ordersMapper;
    private final CurrentUserService currentUserService;

    @GetMapping("/me")
    public HttpResult<AssetVO> me() {
        return HttpResult.success(assetService.me());
    }

    @PostMapping("/recharge")
    public HttpResult<AssetVO> recharge(@RequestParam BigDecimal amount) {
        return HttpResult.success(assetService.recharge(amount));
    }

    @PostMapping("/welcome-coupon")
    public HttpResult<AssetVO> welcomeCoupon() {
        return HttpResult.success(assetService.claimWelcomeCoupon());
    }

    @GetMapping("/coupons")
    public HttpResult<List<UserCoupon>> coupons() {
        return HttpResult.success(assetService.availableCoupons());
    }

    @PostMapping("/membership")
    public HttpResult<AssetVO> membership() {
        return HttpResult.success(assetService.activateMembership());
    }

    @GetMapping("/ledger")
    public HttpResult<List<AssetLedgerVO>> ledger(
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        return HttpResult.success(assetService.ledger(limit));
    }

    @GetMapping("/spending-stats")
    public HttpResult<CustomerStatsVO> spendingStats() {
        CustomerStatsVO stats = ordersMapper.customerSpendingStats(currentUserService.requireUserId());
        return HttpResult.success(stats);
    }
}
