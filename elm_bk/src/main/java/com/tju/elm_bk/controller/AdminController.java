package com.tju.elm_bk.controller;

import com.tju.elm_bk.mapper.BusinessMapper;
import com.tju.elm_bk.mapper.OrdersMapper;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.result.HttpResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "管理端数据看板", description = "管理端数据看板")
public class AdminController {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BusinessMapper businessMapper;
    @Autowired
    private OrdersMapper ordersMapper;

    @GetMapping("/countUser")
    @Operation(summary = "获取总用户数", description = "获取总用户数")
    public HttpResult<Integer> countUser(){
        return HttpResult.success(userMapper.count());
    }

    @GetMapping("/countBusiness")
    @Operation(summary = "获取总店铺数", description = "获取总店铺数")
    public HttpResult<Integer> countBusiness(){
        return HttpResult.success(businessMapper.count());
    }

    @GetMapping("/countPrice")
    @Operation(summary = "获取总营业额", description = "获取总营业额")
    public HttpResult<Double> countPrice(){
        return HttpResult.success(ordersMapper.countPrice());
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('ADMIN')")
    public HttpResult<Map<String,Object>> statistics(@RequestParam(required = false) LocalDate from,
                                                      @RequestParam(required = false) LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().minusDays(30) : from;
        LocalDate end = to == null ? LocalDate.now().plusDays(1) : to.plusDays(1);
        return HttpResult.success(ordersMapper.aggregateStats(start.atStartOfDay(), end.atStartOfDay()));
    }

    @GetMapping(value = "/statistics/export", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> exportStatistics(@RequestParam(required = false) LocalDate from,
                                                     @RequestParam(required = false) LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().minusDays(30) : from;
        LocalDate end = to == null ? LocalDate.now().plusDays(1) : to.plusDays(1);
        Map<String,Object> data = ordersMapper.aggregateStats(start.atStartOfDay(), end.atStartOfDay());
        String csv = "指标,数值\n订单总数," + value(data,"orderCount") + "\n完成订单," + value(data,"completedCount")
                + "\n取消订单," + value(data,"cancelledCount") + "\n异常订单," + value(data,"exceptionCount")
                + "\n有效营业额," + value(data,"revenue") + "\n";
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=statistics.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body("\uFEFF" + csv);
    }

    private Object value(Map<String,Object> data, String key) { return data.getOrDefault(key, 0); }
}
