package com.tju.elm_bk.controller;

import com.tju.elm_bk.entity.Notification;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name="消息管理", description = "获得消息列表与读取消息")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;
    @GetMapping("/notifications")
    @Operation(summary = "获取消息列表")
    public HttpResult<List<Notification>> getNotifications(Long userId){
        return HttpResult.success(notificationService.list(userId));
    }

    @PutMapping("/notifications/{id}/read")
    @Operation(summary = "读取消息")
    public HttpResult readNotification(@PathVariable Long id){
        notificationService.readNotification(id);
        return HttpResult.success();
    }

}
