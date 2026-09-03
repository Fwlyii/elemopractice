package com.tju.elm_bk.service;

import com.tju.elm_bk.constant.OrderStatus;
import com.tju.elm_bk.mapper.DeliveryTaskMapper;
import com.tju.elm_bk.mapper.OrderStatusHistoryMapper;
import com.tju.elm_bk.mapper.OrdersMapper;
import com.tju.elm_bk.mapper.NotificationMapper;
import com.tju.elm_bk.mapper.FoodMapper;
import com.tju.elm_bk.mapper.AssetMapper;
import com.tju.elm_bk.mapper.RiderMapper;
import com.tju.elm_bk.entity.DeliveryTask;
import com.tju.elm_bk.entity.Order;
import com.tju.elm_bk.entity.Notification;
import com.tju.elm_bk.entity.OrderStatusHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {
    private final OrdersMapper ordersMapper;
    private final DeliveryTaskMapper deliveryTaskMapper;
    private final OrderStatusHistoryMapper historyMapper;
    private final NotificationMapper notificationMapper;
    private final FoodMapper foodMapper;
    private final AssetMapper assetMapper;
    private final RiderMapper riderMapper;

    /** 每分钟扫描一次：待支付15分钟自动取消，已送达24小时自动完成。条件更新保证幂等。 */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void closeExpiredOrders() {
        for (Long id : ordersMapper.findExpiredWaitingPaymentIds()) {
            if (ordersMapper.cancelExpiredWaitingPayment(id) == 1) { foodMapper.restoreStockByOrder(id); assetMapper.releaseCouponByOrder(id); ordersMapper.updatePaymentStatus(id, "CANCELLED"); record(id, OrderStatus.WAITING_PAYMENT.getCode(), OrderStatus.CANCELLED.getCode(), "支付超时自动取消"); notifyCustomer(id, "订单支付超时，系统已自动取消"); }
        }
        for (Long id : ordersMapper.findExpiredDeliveredIds()) {
            Order order = ordersMapper.getOrderById(id);
            DeliveryTask task = deliveryTaskMapper.selectByOrderId(id);
            if (ordersMapper.completeExpiredDelivered(id) == 1) {
                deliveryTaskMapper.autoCompleteDelivered(id);
                if (task != null && task.getRiderUserId() != null) {
                    riderMapper.addCompletedStats(task.getRiderUserId(), safe(task.getDistanceKm()), safe(task.getRiderFee()));
                }
                if (order != null && order.getCustomerId() != null) {
                    awardCustomerPoints(order);
                }
                record(id, OrderStatus.DELIVERED.getCode(), OrderStatus.COMPLETED.getCode(), "送达超过24小时自动确认");
                notifyCustomer(id, "订单已送达超过24小时，系统已自动确认收货");
            }
        }
    }

    private void awardCustomerPoints(Order order) {
        assetMapper.ensure(order.getCustomerId());
        int points = order.getOrderTotal() == null
                ? 0 : order.getOrderTotal().setScale(0, RoundingMode.FLOOR).intValue();
        if (points > 0) {
            assetMapper.addPoints(order.getCustomerId(), points);
            assetMapper.insertLedger(order.getCustomerId(), "POINT_EARN", BigDecimal.ZERO, points,
                    "完成订单奖励积分", order.getId());
        }
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void notifyCustomer(Long orderId, String content) {
        var order = ordersMapper.getOrderById(orderId); if (order == null || order.getCustomerId() == null) return;
        Notification n = new Notification(); n.setUserId(order.getCustomerId()); n.setNotificationType(2); n.setNotificationContent(content); n.setIsRead(0); n.setIsDeleted(0); n.setCreateTime(java.time.LocalDateTime.now()); notificationMapper.insert(n);
    }

    private void record(Long orderId, Integer from, Integer to, String reason) {
        OrderStatusHistory h = new OrderStatusHistory(); h.setOrderId(orderId); h.setFromStatus(from); h.setToStatus(to); h.setOperatorUserId(null); h.setReason(reason); historyMapper.insert(h);
        log.info("订单{}状态自动更新为{}", orderId, to);
    }
}
