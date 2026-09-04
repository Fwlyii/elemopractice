package com.tju.elm_bk.service;

import com.tju.elm_bk.constant.OrderStatus;
import com.tju.elm_bk.entity.OrderStatusHistory;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.OrderStatusHistoryMapper;
import com.tju.elm_bk.mapper.OrdersMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 订单状态的原子流转与审计记录。商家、骑手和顾客流程不再各写一遍。
 */
@Service
@RequiredArgsConstructor
public class OrderStateTransitionService {
    private final OrdersMapper ordersMapper;
    private final OrderStatusHistoryMapper historyMapper;

    public void transition(Long orderId, OrderStatus expected, OrderStatus target,
                           Long operatorUserId, String reason) {
        int changed = ordersMapper.updateOrderStateIfCurrent(
                orderId, expected.getCode(), target.getCode(), operatorUserId);
        if (changed != 1) throw new APIException("订单状态已变化，请刷新后重试");
        record(orderId, expected.getCode(), target.getCode(), operatorUserId, reason);
    }

    public void recordCreated(Long orderId, Long operatorUserId) {
        record(orderId, null, OrderStatus.WAITING_PAYMENT.getCode(), operatorUserId, "顾客创建订单");
    }

    private void record(Long orderId, Integer fromStatus, Integer toStatus,
                        Long operatorUserId, String reason) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setOperatorUserId(operatorUserId);
        history.setReason(reason);
        historyMapper.insert(history);
    }
}
