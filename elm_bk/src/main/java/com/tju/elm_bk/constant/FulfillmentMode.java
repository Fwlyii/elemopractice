package com.tju.elm_bk.constant;

import com.tju.elm_bk.exception.APIException;

/** 订单履约方式。数据库统一保存大写枚举名。 */
public enum FulfillmentMode {
    DELIVERY,
    PICKUP;

    public static FulfillmentMode fromClientValue(String value) {
        if (value == null || value.isBlank() || DELIVERY.name().equalsIgnoreCase(value)) {
            return DELIVERY;
        }
        if (PICKUP.name().equalsIgnoreCase(value)) {
            return PICKUP;
        }
        throw new APIException("不支持的履约方式，请选择外送或自取");
    }

    public boolean requiresAddress() {
        return this == DELIVERY;
    }

    public boolean chargesDeliveryFee() {
        return this == DELIVERY;
    }
}
