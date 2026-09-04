package com.tju.elm_bk.service;

import com.tju.elm_bk.entity.Order;
import com.tju.elm_bk.mapper.AssetMapper;
import com.tju.elm_bk.mapper.FoodMapper;
import com.tju.elm_bk.mapper.OrdersMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class OrderSettlementServiceTest {
    private AssetMapper assetMapper;
    private OrdersMapper ordersMapper;
    private FoodMapper foodMapper;
    private AssetService assetService;
    private OrderSettlementService settlementService;

    @BeforeEach
    void setUp() {
        assetMapper = mock(AssetMapper.class);
        ordersMapper = mock(OrdersMapper.class);
        foodMapper = mock(FoodMapper.class);
        assetService = mock(AssetService.class);
        settlementService = new OrderSettlementService(assetMapper, ordersMapper, foodMapper, assetService);
    }

    @Test
    void paidCancellationRestoresEveryReservedResource() {
        Order order = order(18L, 7L, new BigDecimal("26.50"), 300, true);

        settlementService.cancelOrder(order, true);

        verify(foodMapper).restoreStockByOrder(18L);
        verify(assetMapper).releaseCouponByOrder(18L);
        verify(ordersMapper).updatePaymentStatus(18L, "REFUNDED");
        verify(assetService).refundOrderAssets(18L, 7L, 300, new BigDecimal("26.50"));
    }

    @Test
    void unpaidCancellationDoesNotInventARefund() {
        Order order = order(19L, 7L, new BigDecimal("19.00"), 0, false);

        settlementService.cancelOrder(order, false);

        verify(foodMapper).restoreStockByOrder(19L);
        verify(assetMapper).releaseCouponByOrder(19L);
        verify(ordersMapper).updatePaymentStatus(19L, "CANCELLED");
        verifyNoInteractions(assetService);
    }

    @Test
    void completionPointsUseTheActuallyPaidAmount() {
        Order order = order(20L, 8L, new BigDecimal("32.80"), 0, false);

        settlementService.awardCompletionPoints(order, 8L);

        verify(assetMapper).ensure(8L);
        verify(assetMapper).addPoints(8L, 32);
        verify(assetMapper).insertLedger(
                8L, "POINT_EARN", BigDecimal.ZERO, 32, "完成订单奖励积分", 20L);
    }

    private Order order(Long id, Long customerId, BigDecimal total, Integer pointsUsed, boolean walletPaid) {
        Order order = new Order();
        order.setId(id);
        order.setCustomerId(customerId);
        order.setOrderTotal(total);
        order.setPointsUsed(pointsUsed);
        order.setWalletPaid(walletPaid);
        return order;
    }
}
