package com.tju.elm_bk.service;

import com.tju.elm_bk.dto.DeliveryExceptionCreateDTO;
import com.tju.elm_bk.dto.DeliveryExceptionResolveDTO;
import com.tju.elm_bk.entity.DeliveryException;
import com.tju.elm_bk.entity.OrderStatusHistory;
import com.tju.elm_bk.vo.DeliveryExceptionVO;
import com.tju.elm_bk.vo.DeliveryTaskVO;

import java.util.List;

public interface DeliveryService {
    List<DeliveryTaskVO> listAvailableTasks();

    List<DeliveryTaskVO> listMyTasks(Boolean active);

    DeliveryTaskVO getTask(Long taskId);

    DeliveryTaskVO getOrderDelivery(Long orderId);

    DeliveryTaskVO merchantAcceptOrder(Long orderId);
    DeliveryTaskVO merchantReadyOrder(Long orderId);

    void merchantRejectOrder(Long orderId);

    DeliveryTaskVO acceptTask(Long taskId);

    DeliveryTaskVO arriveStore(Long taskId);

    DeliveryTaskVO pickup(Long taskId);

    DeliveryTaskVO deliver(Long taskId);

    DeliveryTaskVO confirmReceipt(Long orderId);

    DeliveryException reportException(Long taskId, DeliveryExceptionCreateDTO dto);

    List<DeliveryExceptionVO> listExceptions(Integer status);

    DeliveryTaskVO resolveException(Long exceptionId, DeliveryExceptionResolveDTO dto);

    List<OrderStatusHistory> listOrderHistory(Long orderId);
}
