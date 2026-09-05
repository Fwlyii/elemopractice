package com.tju.elm_bk.result;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class HttpResult<T> implements Serializable {
    private final boolean success;
    private final String code;
    private final T data;
    private final String message;

    private HttpResult(boolean success, String code, T data, String message) {
        this.success = success;
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public static <T> HttpResult<T> success() {
        return success(null);
    }

    public static <T> HttpResult<T> success(T data) {
        return new HttpResult<>(true, ResultCodeEnum.SUCCESS.getCode(), data, null);
    }

    public static <T> HttpResult<T> failure(ResultCodeEnum resultCode) {
        return failure(resultCode, resultCode.getMessage());
    }

    public static <T> HttpResult<T> failure(ResultCodeEnum resultCode, String message) {
        return failure(resultCode.getCode(), message);
    }

    public static <T> HttpResult<T> failure(String code, String message) {
        return new HttpResult<>(false, code, null, message);
    }
}
