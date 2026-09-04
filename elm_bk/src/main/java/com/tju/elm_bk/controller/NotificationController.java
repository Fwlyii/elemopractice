package com.tju.elm_bk.controller;

import com.tju.elm_bk.entity.Notification;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.service.CurrentUserService;
import com.tju.elm_bk.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name="消息管理", description = "获得消息列表与读取消息")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;
    @GetMapping("/notifications")
    @Operation(summary = "获取消息列表")
    public HttpResult<List<Notification>> getNotifications(Long userId){
        Long currentUserId = currentUserId();
        // 忽略普通用户传入的 userId，避免通过改参数读取他人消息；管理员也默认查看自己的消息。
        return HttpResult.success(notificationService.list(currentUserId));
    }

    @PutMapping("/notifications/{id}/read")
    @Operation(summary = "读取消息")
    public HttpResult readNotification(@PathVariable Long id){
        notificationService.readNotification(id, currentUserId());
        return HttpResult.success();
    }

    private Long currentUserId() {
        return currentUserService.requireUserId();
    }

}
