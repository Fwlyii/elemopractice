package com.tju.elm_bk.service;

import com.tju.elm_bk.entity.Notification;

import java.util.List;

public interface NotificationService {
    List<Notification> list(Long userId);

    void readNotification(Long id);
}
