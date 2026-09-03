package com.tju.elm_bk.constant;

import com.tju.elm_bk.exception.APIException;

import java.util.Arrays;
import java.util.Set;

/** 配送任务状态及其合法流转。 */
public enum DeliveryTaskStatus {
    WAITING_RIDER("待骑手接单"),
    ACCEPTED("已接单"),
    ARRIVED_STORE("已到店"),
    DELIVERING("配送中"),
    DELIVERED("已送达"),
    COMPLETED("已完成"),
    EXCEPTION("配送异常"),
    CANCELLED("已取消");

    private final String label;

    DeliveryTaskStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static DeliveryTaskStatus fromName(String value) {
        return Arrays.stream(values())
                .filter(status -> status.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new APIException("未知配送状态：" + value));
    }

    public boolean canTransitionTo(DeliveryTaskStatus target) {
        return switch (this) {
            case WAITING_RIDER -> target == ACCEPTED || target == CANCELLED;
            case ACCEPTED -> Set.of(ARRIVED_STORE, EXCEPTION, CANCELLED).contains(target);
            case ARRIVED_STORE -> Set.of(DELIVERING, EXCEPTION, CANCELLED).contains(target);
            case DELIVERING -> Set.of(DELIVERED, EXCEPTION).contains(target);
            case DELIVERED -> target == COMPLETED || target == EXCEPTION;
            case EXCEPTION -> Set.of(WAITING_RIDER, ACCEPTED, ARRIVED_STORE, DELIVERING, CANCELLED).contains(target);
            case COMPLETED, CANCELLED -> false;
        };
    }
}
