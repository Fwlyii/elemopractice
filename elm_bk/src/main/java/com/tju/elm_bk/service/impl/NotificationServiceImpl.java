package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.entity.Notification;
import com.tju.elm_bk.mapper.NotificationMapper;
import com.tju.elm_bk.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationMapper notificationMapper;
    @Override
    public List<Notification> list(Long userId) {
        log.info("获取用户{}的通知列表", userId);
        return notificationMapper.list(userId);
    }

    @Override
    public void readNotification(Long id, Long userId) {
        notificationMapper.updateRead(id, userId, LocalDateTime.now());
    }
}
