package com.tju.elm_bk.controller;

import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.result.ResultCodeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@Tag(name="其他接口")
public class CommonController {

    @GetMapping("/httpRest/success")
    @Operation(summary = "通用返回成功(没有返回结果)")
    public HttpResult<Object> successWithoutData() {
        return HttpResult.success();
    }

    @GetMapping("/httpRest/successWithData")
    @Operation(summary = "通用返回成功(有返回结果)")
    public HttpResult<Object> successWithData() {
        Object data = new Object();
        return HttpResult.success(data);
    }

    @GetMapping("/httpRest/failure")
    @Operation(summary = "通用返回失败")
    public HttpResult<Object> failure() {
        return HttpResult.failure(ResultCodeEnum.COMMON_ERROR);
    }
}
