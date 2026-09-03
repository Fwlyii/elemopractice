package com.tju.elm_bk.constant;

import com.tju.elm_bk.exception.APIException;

import java.util.Arrays;

/**
 * 订单履约状态。状态码同时写入数据库，前后端统一使用本枚举定义的数值。
 */
public enum OrderStatus {
    WAITING_PAYMENT(0, "待支付"),
    WAITING_MERCHANT_ACCEPT(1, "待商家接单"),
    WAITING_DISPATCH(2, "待派单"),
    WAITING_RIDER_ACCEPT(3, "待骑手接单"),
    WAITING_PICKUP(4, "待取餐"),
    DELIVERING(5, "配送中"),
    DELIVERED(6, "已送达"),
    COMPLETED(7, "已完成"),
    CANCELLED(8, "已取消"),
    DELIVERY_EXCEPTION(9, "配送异常");

    private final int code;
    private final String label;

    OrderStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static OrderStatus fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElseThrow(() -> new APIException("未知订单状态：" + code));
    }
}
