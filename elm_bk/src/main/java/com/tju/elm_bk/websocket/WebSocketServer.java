package com.tju.elm_bk.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@ServerEndpoint("/ws/{sid}")
public class WebSocketServer {

    // 线程安全的Map存储会话（key: sid，value: 会话）
    private static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    private static final Lock lock = new ReentrantLock(); // 保证群发消息的线程安全

    /**
     * 连接建立成功
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        System.out.println("客户端：" + sid + " 建立连接");
        sessionMap.put(sid, session);
    }

    /**
     * 接收客户端消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        System.out.println("收到客户端【" + sid + "】的消息：" + message);
    }

    /**
     * 连接关闭
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        System.out.println("客户端：" + sid + " 断开连接");
        sessionMap.remove(sid);
    }

    /**
     * 连接异常
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket连接异常：" + error.getMessage());
        error.printStackTrace();
    }

    /**
     * 群发消息（线程安全）
     * @param message 消息内容（JSON格式）
     */
    public void sendToAllClient(String message) {
        lock.lock(); // 加锁防止并发问题
        try {
            Collection<Session> sessions = sessionMap.values();
            for (Session session : sessions) {
                if (session.isOpen()) { // 只给打开的连接发送消息
                    session.getBasicRemote().sendText(message);
                }
            }
        } catch (Exception e) {
            System.err.println("WebSocket群发消息失败：" + e.getMessage());
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
            Session session = sessionMap.get(sid);
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendText(message);
            }
        } catch (Exception e) {
            System.err.println("WebSocket单发消息失败：" + e.getMessage());
        } finally {
            lock.unlock();
        }
    }
}
