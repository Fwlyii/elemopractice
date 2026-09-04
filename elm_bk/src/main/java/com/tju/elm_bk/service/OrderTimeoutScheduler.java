package com.tju.elm_bk.service;

import com.tju.elm_bk.constant.OrderStatus;
import com.tju.elm_bk.mapper.DeliveryTaskMapper;
import com.tju.elm_bk.mapper.OrderStatusHistoryMapper;
import com.tju.elm_bk.mapper.OrdersMapper;
import com.tju.elm_bk.mapper.NotificationMapper;
import com.tju.elm_bk.mapper.RiderMapper;
import com.tju.elm_bk.entity.DeliveryTask;
import com.tju.elm_bk.entity.Order;
import com.tju.elm_bk.entity.Notification;
import com.tju.elm_bk.entity.OrderStatusHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OrderTimeoutScheduler {
    private final OrdersMapper ordersMapper;
    private final DeliveryTaskMapper deliveryTaskMapper;
    private final OrderStatusHistoryMapper historyMapper;
    private final NotificationMapper notificationMapper;
    private final RiderMapper riderMapper;
    private final OrderSettlementService orderSettlementService;

    /** 每分钟扫描一次：待支付15分钟自动取消，已送达24小时自动完成。条件更新保证幂等。 */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void closeExpiredOrders() {
        for (Long id : ordersMapper.findExpiredWaitingPaymentIds()) {
            Order order = ordersMapper.getOrderById(id);
            if (order != null && ordersMapper.cancelExpiredWaitingPayment(id) == 1) {
                orderSettlementService.cancelOrder(order, false);
                record(id, OrderStatus.WAITING_PAYMENT.getCode(), OrderStatus.CANCELLED.getCode(), "支付超时自动取消");
                notifyCustomer(id, "订单支付超时，系统已自动取消");
            }
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
                    orderSettlementService.awardCompletionPoints(order, order.getCustomerId());
                }
                record(id, OrderStatus.DELIVERED.getCode(), OrderStatus.COMPLETED.getCode(), "送达超过24小时自动确认");
                notifyCustomer(id, "订单已送达超过24小时，系统已自动确认收货");
            }
        }
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void notifyCustomer(Long orderId, String content) {
        Order order = ordersMapper.getOrderById(orderId);
        if (order == null || order.getCustomerId() == null) return;

        Notification notification = new Notification();
        notification.setUserId(order.getCustomerId());
        notification.setNotificationType(2);
        notification.setNotificationContent(content);
        notification.setIsRead(0);
        notification.setIsDeleted(0);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);
    }

    private void record(Long orderId, Integer from, Integer to, String reason) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setOperatorUserId(null);
        history.setReason(reason);
        historyMapper.insert(history);
        log.info("订单{}状态自动更新为{}", orderId, to);
    }
}
