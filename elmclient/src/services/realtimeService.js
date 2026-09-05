import { getStoredUser, getToken } from '@/utils/auth';
import { getWebSocketUrl } from '@/utils/endpoints';

export const REALTIME_STATE = Object.freeze({
  IDLE: 'idle',
  CONNECTING: 'connecting',
  CONNECTED: 'connected',
  RECONNECTING: 'reconnecting',
  OFFLINE: 'offline',
  UNAUTHORIZED: 'unauthorized'
});

const normalClose = 1000;
const unauthorizedClose = 1008;

/**
 * 全站唯一的实时消息连接实现。
 *
 * 页面只提供“收到消息后做什么”和“降级轮询时做什么”，不再各自拼接
 * WebSocket 地址、用户编号和重连定时器。后端要求 /ws/{userId} 中的
 * userId 必须与 JWT 对应账号一致，这里统一从登录态读取，避免随机 sid
 * 导致合法连接被后端拒绝。
 */
export const createRealtimeConnection = ({
  onMessage,
  onStatusChange,
  onFallbackRefresh,
  fallbackIntervalMs = 15000,
  maxReconnectDelayMs = 15000
} = {}) => {
  let socket = null;
  let stopped = true;
  let reconnectAttempts = 0;
  let reconnectTimer = null;
  let fallbackTimer = null;
  let currentState = REALTIME_STATE.IDLE;

  const emitStatus = (state, detail = {}) => {
    currentState = state;
    onStatusChange?.({ state, ...detail });
  };

  const runFallbackRefresh = () => {
    if (!navigator.onLine || typeof onFallbackRefresh !== 'function') return;
    Promise.resolve(onFallbackRefresh()).catch(() => {
      // 降级刷新失败由下一轮继续尝试，不能制造重复 toast。
    });
  };

  const startFallbackRefresh = () => {
    if (fallbackTimer || typeof onFallbackRefresh !== 'function') return;
    fallbackTimer = window.setInterval(runFallbackRefresh, fallbackIntervalMs);
  };

  const stopFallbackRefresh = () => {
    if (!fallbackTimer) return;
    window.clearInterval(fallbackTimer);
    fallbackTimer = null;
  };

  const clearReconnectTimer = () => {
    if (!reconnectTimer) return;
    window.clearTimeout(reconnectTimer);
    reconnectTimer = null;
  };

  const scheduleReconnect = () => {
    if (stopped || reconnectTimer || !navigator.onLine) return;
    const delay = Math.min(1000 * (2 ** reconnectAttempts), maxReconnectDelayMs);
    reconnectAttempts += 1;
    emitStatus(REALTIME_STATE.RECONNECTING, { retryInMs: delay });
    reconnectTimer = window.setTimeout(() => {
      reconnectTimer = null;
      connect();
    }, delay);
  };

  const handleOnline = () => {
    if (stopped || currentState === REALTIME_STATE.CONNECTED) return;
    reconnectAttempts = 0;
    clearReconnectTimer();
    connect();
  };

  const handleOffline = () => {
    clearReconnectTimer();
    emitStatus(REALTIME_STATE.OFFLINE);
    startFallbackRefresh();
    if (socket && socket.readyState < WebSocket.CLOSING) socket.close(normalClose, 'offline');
  };

  const connect = () => {
    if (stopped) return;
    if (!navigator.onLine) {
      emitStatus(REALTIME_STATE.OFFLINE);
      startFallbackRefresh();
      return;
    }
    if (socket && [WebSocket.CONNECTING, WebSocket.OPEN].includes(socket.readyState)) return;

    const token = getToken();
    const userId = getStoredUser()?.id;
    if (!token || !userId) {
      emitStatus(REALTIME_STATE.UNAUTHORIZED);
      stopFallbackRefresh();
      return;
    }

    emitStatus(reconnectAttempts ? REALTIME_STATE.RECONNECTING : REALTIME_STATE.CONNECTING);
    const connection = new WebSocket(getWebSocketUrl(`/ws/${encodeURIComponent(userId)}`));
    socket = connection;

    connection.onopen = () => {
      if (connection !== socket || stopped) return;
      reconnectAttempts = 0;
      stopFallbackRefresh();
      emitStatus(REALTIME_STATE.CONNECTED);
    };

    connection.onmessage = event => {
      if (connection !== socket || stopped) return;
      try {
        const payload = JSON.parse(event.data);
        onMessage?.(payload, event);
      } catch (_) {
        // 非 JSON 推送不参与页面业务，避免一条异常消息中断后续连接。
      }
    };

    connection.onerror = () => {
      if (connection !== socket || stopped) return;
      startFallbackRefresh();
    };

    connection.onclose = event => {
      if (connection !== socket) return;
      socket = null;
      if (stopped) return;
      startFallbackRefresh();
      if (event.code === unauthorizedClose) {
        emitStatus(REALTIME_STATE.UNAUTHORIZED);
        return;
      }
      if (!navigator.onLine) {
        emitStatus(REALTIME_STATE.OFFLINE);
        return;
      }
      scheduleReconnect();
    };
  };

  const start = () => {
    if (!stopped) return;
    stopped = false;
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    connect();
  };

  const retry = () => {
    if (stopped) return;
    reconnectAttempts = 0;
    clearReconnectTimer();
    if (socket) {
      const previous = socket;
      socket = null;
      previous.close(normalClose, 'manual retry');
    }
    connect();
  };

  const stop = () => {
    if (stopped) return;
    stopped = true;
    clearReconnectTimer();
    stopFallbackRefresh();
    window.removeEventListener('online', handleOnline);
    window.removeEventListener('offline', handleOffline);
    if (socket) {
      const activeSocket = socket;
      socket = null;
      activeSocket.close(normalClose, 'component unmounted');
    }
    emitStatus(REALTIME_STATE.IDLE);
  };

  return {
    start,
    retry,
    stop,
    getState: () => currentState
  };
};
