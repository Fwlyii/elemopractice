package com.tju.elm_bk.service;

import com.alibaba.fastjson.JSONObject;
import com.tju.elm_bk.constant.AuthorityName;
import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.entity.Notification;
import com.tju.elm_bk.entity.Order;
import com.tju.elm_bk.mapper.BusinessMapper;
import com.tju.elm_bk.mapper.NotificationMapper;
import com.tju.elm_bk.websocket.WebSocketServer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 配送消息出口：数据库通知和 WebSocket 推送在同一处组装。
 */
@Service
@RequiredArgsConstructor
public class DeliveryNotificationService {
    private final NotificationMapper notificationMapper;
    private final BusinessMapper businessMapper;
    private final WebSocketServer webSocketServer;

    public void notifyOrderParties(Order order, Long riderUserId, String content) {
        notifyUser(order.getCustomerId(), content, order.getId());
        notifyMerchant(order, content);
        if (riderUserId != null) notifyUser(riderUserId, content, order.getId());
    }

    public void notifyMerchant(Order order, String content) {
        Business business = businessMapper.selectBusinessById(order.getBusinessId());
        if (business != null) notifyUser(business.getUserId(), content, order.getId());
    }

    public void notifyUser(Long userId, String content, Long orderId) {
        if (userId == null) return;

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setNotificationType(3);
        notification.setNotificationContent(content + "（订单 #" + orderId + "）");
        notification.setAuditResult(0);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        notification.setIsDeleted(0);
        notificationMapper.insert(notification);

        webSocketServer.sendToClient(userId.toString(), message(content, orderId).toJSONString());
    }

    public void notifyAudience(AuthorityName authority, String content, Long orderId) {
        webSocketServer.sendToAuthority(authority.name(), message(content, orderId).toJSONString());
    }

    private JSONObject message(String content, Long orderId) {
        JSONObject message = new JSONObject();
        message.put("type", "delivery_update");
        message.put("orderId", orderId);
        message.put("content", content);
        return message;
    }
}
