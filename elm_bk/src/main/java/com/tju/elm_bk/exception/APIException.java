package com.tju.elm_bk.exception;

import com.tju.elm_bk.result.ResultCodeEnum;
import lombok.Getter;

// 接口错误时统一抛出APIException 由全局异常处理器统一捕获
@Getter
public class APIException extends RuntimeException {
    private String code;

    public APIException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
    }

    public APIException(String message) {
        super(message);
    }

    public APIException(String code, String message) {
        super(message);
        this.code = code;
    }
}
