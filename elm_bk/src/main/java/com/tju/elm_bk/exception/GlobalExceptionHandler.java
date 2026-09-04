package com.tju.elm_bk.exception;

import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.result.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler
    public void clientAbortExceptionHandler(ClientAbortException clientAbortException) {
        log.warn(ResultCodeEnum.CLIENT_ABORT.getMessage());
    }

    // query参数不全
    @ExceptionHandler
    public ResponseEntity<HttpResult<Object>> apiExceptionHandler(MissingServletRequestParameterException paramException) {
        return ResponseEntity.badRequest().body(HttpResult.failure(ResultCodeEnum.PARAM_NOT_MATCHED_GET));
    }
    // body参数不全
    @ExceptionHandler
    public ResponseEntity<HttpResult<Object>> apiExceptionHandler(HttpMessageNotReadableException messageNotReadableException) {
        return ResponseEntity.badRequest().body(HttpResult.failure(ResultCodeEnum.PARAM_NOT_MATCHED_POST));
    }
    // 参数不匹配
    @ExceptionHandler
    public ResponseEntity<HttpResult<Object>> apiExceptionHandler(MethodArgumentTypeMismatchException methodArgumentTypeMismatchException) {
        log.warn("请求参数类型不匹配: {}", methodArgumentTypeMismatchException.getName());
        return ResponseEntity.badRequest().body(HttpResult.failure(ResultCodeEnum.PARAM_NOT_MATCHED));
    }
    // 请求类型不支持
    @ExceptionHandler
    public ResponseEntity<HttpResult<Object>> apiExceptionHandler(HttpRequestMethodNotSupportedException httpRequestMethodNotSupportedException) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(HttpResult.failure(ResultCodeEnum.NOT_SUPPORTED));
    }
    // 参数校验不对
    @ExceptionHandler
    public ResponseEntity<HttpResult<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(HttpResult.failure("PARAM_VERIFIED_FAILED",errors.toString()));
    }

    /**
     * Method-level authorization failures must keep their HTTP 403 semantics.
     * Otherwise the generic exception handler below would wrap them in a 200 response.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<HttpResult<Object>> accessDeniedHandler(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(HttpResult.failure(ResultCodeEnum.UNAUTHORIZED));
    }

    /**
     * 业务异常不能统一返回 200，否则调用方只能解析响应体才能知道请求失败。
     * 根据错误类型保留统一 HttpResult，同时返回符合 REST 语义的 HTTP 状态码。
     */
    @ExceptionHandler(APIException.class)
    public ResponseEntity<HttpResult<Object>> apiExceptionHandler(APIException ex) {
        String code = ex.getCode();
        HttpResult<Object> body = code == null
                ? HttpResult.failure(ResultCodeEnum.WITHOUT_ERROR_CODE.getCode(), ex.getMessage())
                : HttpResult.failure(code, ex.getMessage());
        return ResponseEntity.status(httpStatusFor(code)).body(body);
    }

    private HttpStatus httpStatusFor(String code) {
        if (code == null || ResultCodeEnum.WITHOUT_ERROR_CODE.getCode().equals(code)) {
            // 订单状态冲突、重复操作等没有细分错误码的业务异常
            return HttpStatus.CONFLICT;
        }
        return switch (code) {
            case "UNAUTHORIZED", "NOT_ENOUGH_PERMISSION", "USER_DENIED",
                    "ADDRESS_PERMISSION_DENIED", "USER_UNMATCHED" -> HttpStatus.FORBIDDEN;
            case "NOT_FOUND", "USER_MISSED", "BUSINESS_MISSED", "FOOD_MISSED",
                    "ORDER_MISSED", "ADDRESS_MISSED", "CART_MISSED" -> HttpStatus.NOT_FOUND;
            case "SERVER_ERROR", "GENERAL_ERROR", "NOT_KNOWN_ERROR" -> HttpStatus.INTERNAL_SERVER_ERROR;
            case "PARAM_NOT_MATCHED", "PARAM_NOT_MATCHED_GET", "PARAM_NOT_MATCHED_POST",
                    "PARAM_VERIFIED_FAILED", "NOT_SUPPORTED" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }

    /**
     * 捕获业务逻辑异常
     * @param ex 异常
     */
    @ExceptionHandler
    public ResponseEntity<HttpResult<Object>> exceptionHandler(Exception ex) {
        log.error("异常为：{}",ex.getClass());
        log.error("堆栈信息：", ex);
        // 未知异常只记录在服务端，不把 SQL、类名或内部状态返回给客户端。
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(HttpResult.failure(ResultCodeEnum.NOT_KNOWN_ERROR));
    }

}
