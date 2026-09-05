package com.tju.elm_bk.websocket;

import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.security.TokenProvider;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@ServerEndpoint("/ws/{sid}")
@Slf4j
public class WebSocketServer {

    // 同一账号可同时打开多个页面；不能让后建立的连接覆盖原会话。
    private static final Map<String, Set<Session>> sessionMap = new ConcurrentHashMap<>();
    private static final Lock lock = new ReentrantLock(); // 保证群发消息的线程安全
    private static volatile TokenProvider tokenProvider;
    private static volatile UserMapper userMapper;

    @Autowired
    public void configureSecurity(TokenProvider provider, UserMapper mapper) {
        WebSocketServer.tokenProvider = provider;
        WebSocketServer.userMapper = mapper;
    }

    /**
     * 连接建立成功
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        if (!authenticate(session, sid)) {
            closeUnauthorized(session);
            return;
        }
        sessionMap.computeIfAbsent(sid, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    /**
     * 接收客户端消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        // 当前通道仅用于服务端推送，客户端消息不参与任何业务操作。
    }

    /**
     * 连接关闭
     */
    @OnClose
    public void onClose(Session session, @PathParam("sid") String sid) {
        sessionMap.computeIfPresent(sid, (key, sessions) -> {
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    /**
     * 连接异常
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.warn("WebSocket连接异常: {}", error == null ? "unknown" : error.getClass().getSimpleName());
    }

    /**
     * 群发消息（线程安全）
     * @param message 消息内容（JSON格式）
     */
    public void sendToAllClient(String message) {
        lock.lock(); // 加锁防止并发问题
        try {
            Collection<Set<Session>> sessionGroups = sessionMap.values();
            for (Set<Session> sessions : sessionGroups) {
                for (Session session : sessions) {
                    if (session.isOpen()) {
                        session.getBasicRemote().sendText(message);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("WebSocket群发消息失败: {}", e.getClass().getSimpleName());
        } finally {
            lock.unlock(); // 释放锁
        }
    }

    /**
     * 给指定用户发送消息
     */
    public void sendToClient(String sid, String message) {
        lock.lock();
        try {
            Set<Session> sessions = sessionMap.get(sid);
            if (sessions != null) {
                for (Session session : sessions) {
                    if (session.isOpen()) {
                        session.getBasicRemote().sendText(message);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("WebSocket单发消息失败: {}", e.getClass().getSimpleName());
        } finally {
            lock.unlock();
        }
    }

    public void sendToAuthority(String authority, String message) {
        UserMapper mapper = userMapper;
        if (mapper == null || authority == null) return;
        List<Long> userIds = mapper.findUserIdsByAuthority(authority);
        if (userIds != null) {
            userIds.forEach(id -> sendToClient(id.toString(), message));
        }
    }

    private boolean authenticate(Session session, String sid) {
        try {
            TokenProvider provider = tokenProvider;
            UserMapper mapper = userMapper;
            List<String> tokens = session.getRequestParameterMap().get("access_token");
            if (provider == null || mapper == null || tokens == null || tokens.size() != 1) return false;
            String token = tokens.get(0);
            if (!provider.validateToken(token)) return false;
            String username = provider.getAuthentication(token).getName();
            User user = mapper.findByUsernameWithAuthorities(username);
            return user != null && Boolean.TRUE.equals(user.getActivated())
                    && provider.isCurrentForAccount(token, user.getUpdateTime())
                    && provider.isRoleBoundAndCurrentForAccount(token, user)
                    && Objects.equals(String.valueOf(user.getId()), sid);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void closeUnauthorized(Session session) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "身份验证失败"));
        } catch (Exception ignored) {
            // 连接本身可能已被客户端关闭。
        }
    }
}
