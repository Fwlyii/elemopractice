package com.tju.elm_bk.exception;

/**
 * HttpUtil工具类出错时抛出的异常
 */
public class HttpUtilException extends RuntimeException {
    public HttpUtilException(String message) {
        super(message);
    }
}
