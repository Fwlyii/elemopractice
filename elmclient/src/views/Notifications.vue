<template>
  <div class="notifications-container">
    <div class="header">
      <h1 class="title">消息与通知</h1>
      <div v-if="unreadCount > 0" class="unread-badge">{{ unreadCount }}</div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载消息中...</p>
    </div>

    <!-- 错误状态 -->
    <div v-if="error" class="error-state">
      <i class="fas fa-exclamation-circle"></i>
      <p>{{ error }}</p>
      <button class="retry-btn" @click="initWebSocket">重新连接</button>
    </div>

    <!-- 消息列表 -->
    <div v-else class="notification-list">
      <div v-for="message in messages" :key="message.id" class="notification-item" @click="markAsRead(message)">
        <div class="icon-wrapper" :class="{ 'unread': message.unread }">
          <svg class="icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
            <path d="M22 6.5C22 7.32843 21.3284 8 20.5 8H3.5C2.67157 8 2 7.32843 2 6.5V3.5C2 2.67157 2.67157 2 3.5 2H20.5C21.3284 2 22 2.67157 22 3.5V6.5ZM22 17.5C22 18.3284 21.3284 19 20.5 19H3.5C2.67157 19 2 18.3284 2 17.5V14.5C2 13.6716 2.67157 13 3.5 13H20.5C21.3284 13 22 13.6716 22 14.5V17.5ZM2 10.5C2 9.67157 2.67157 9 3.5 9H20.5C21.3284 9 22 9.67157 22 10.5V11.5C22 12.3284 21.3284 13 20.5 13H3.5C2.67157 13 2 12.3284 2 11.5V10.5Z"></path>
          </svg>
        </div>
        <div class="content">
          <div class="message-text" :class="{ 'bold': message.unread }">{{ message.notificationContent }}</div>
          <div class="timestamp">{{ formatTime(message.currentTime) }}</div>
        </div>
        <div v-if="message.unread" class="dot"></div>
      </div>

      <!-- 空状态 -->
      <div v-if="messages.length === 0" class="empty-state">
        <i class="fas fa-envelope-open"></i>
        <p>暂无消息通知</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue';
import request from '@/utils/request';
import { toast } from '@/utils/toast';
import { getWebSocketUrl } from '@/utils/endpoints';

// 状态管理
const messages = ref([]);
const loading = ref(true);
const error = ref('');
const webSocket = ref(null);
const isConnected = ref(false);

const userFromLocal = localStorage.getItem('userInfo') ? JSON.parse(localStorage.getItem('userInfo')) : null;
const userFromSession = sessionStorage.getItem('userInfo') ? JSON.parse(sessionStorage.getItem('userInfo')) : null;
const	user = userFromLocal || userFromSession;
const currentUserId = user.id;

// 计算未读消息数量
const unreadCount = computed(() => {
  return messages.value.filter(msg => msg.unread).length;
});

// 初始化：加载历史消息并连接WebSocket
onMounted(() => {
  fetchHistoryMessages();
  initWebSocket();
});

// 组件卸载时关闭WebSocket连接
onUnmounted(() => {
  if (webSocket.value) {
    webSocket.value.close();
  }
});

/**
 * 加载历史消息
 */
const fetchHistoryMessages = async () => {
  try {
    const res = await request.get('/api/notifications', {
      params: {
        userId: currentUserId // 假设 currentUserId 是当前用户的 ID
      }
    });
    if (res.success) {
      messages.value = res.data.map(msg => ({
        ...msg,
        unread: msg.isRead !== 1, // isRead=0 → 未读 → unread=true；isRead=1 → 已读 → unread=false
        createTime: msg.createTime
      }));
    }
  } catch (err) {
    console.error('加载历史消息失败:', err);
    toast.error('加载消息失败');
  } finally {
    loading.value = false;
  }
};

/**
 * 初始化WebSocket连接
 */
const initWebSocket = () => {
  // 清除之前的连接
  if (webSocket.value) {
    webSocket.value.close();
  }

  error.value = '';
  loading.value = true;

  try {
    // // 获取当前用户ID，如果未登录则不连接
    // if (!currentUserId.value) {
    //   error.value = '请先登录以接收消息通知';
    //   loading.value = false;
    //   return;
    // }

    // 创建WebSocket连接
    const sid = `client-${Date.now()}-${Math.floor(Math.random() * 1000)}`;
    const wsUrl = getWebSocketUrl(`/ws/${sid}`);
    
    webSocket.value = new WebSocket(wsUrl);

    // 连接成功
    webSocket.value.onopen = () => {
      console.log('WebSocket连接成功');
      isConnected.value = true;
      error.value = '';
      loading.value = false;
      //toast.success('已连接消息通知');
    };

    // 接收消息
    webSocket.value.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        handleNewMessage(message);
      } catch (err) {
        console.error('解析消息失败:', err);
      }
    };

    // 连接关闭
    webSocket.value.onclose = (event) => {
      console.log('WebSocket连接关闭，代码:', event.code);
      isConnected.value = false;
      
      // 不是手动关闭的连接，尝试重连
      if (event.code !== 1000) {
        error.value = '消息连接已断开，正在尝试重连...';
        // 3秒后重连
        setTimeout(initWebSocket, 3000);
      }
    };

    // 连接错误
    webSocket.value.onerror = (err) => {
      console.error('WebSocket错误:', err);
      isConnected.value = false;
      error.value = '消息连接出错，请检查网络';
      loading.value = false;
    };
  } catch (err) {
    console.error('初始化WebSocket失败:', err);
    error.value = '无法建立消息连接';
    loading.value = false;
  }
};

/**
 * 处理新接收的消息
 */
const handleNewMessage = (message) => {
  // 只处理当前用户的消息
  if (message.userId && message.userId !== currentUserId.value) {
    return;
  }
  // 2. 验证消息合法性（确保是审核相关的有效消息）
  const isValidMessage = message.type !== undefined && message.content;
  if (!isValidMessage) {
    console.warn('收到无效的WebSocket消息:', message);
    return;
  }

  // 3. 显示即时提示（告知用户有新消息）
  toast.info(`新消息：${message.notificationContent}`);

  // 4. 延迟调用接口重新拉取消息列表（避免后端写入数据库延迟导致漏消息）
  // 延迟300ms是为了确保后端已将新消息写入数据库，再前端拉取
  setTimeout(async () => {
    try {
      // 5. 调用接口重新获取完整消息列表（复用加载历史消息的逻辑，确保数据一致）
      const res = await request.get('/api/notifications');
      if (res.success) {
        // 6. 重新映射消息状态（isRead → unread），覆盖本地列表
        messages.value = res.data.map(msg => ({
          ...msg,
          // 后端isRead=0→未读（unread=true），isRead=1→已读（unread=false）
          unread: msg.isRead !== 1,
          createTime: msg.createTime
        }));
      }
    } catch (err) {
      console.error('WebSocket消息触发重新拉取失败:', err);
      // 失败时给用户提示，允许手动重试
      toast.error('新消息已收到，但加载失败，可下拉刷新重试');
    }
  }, 300);
};

/**
 * 标记消息为已读
 */
const markAsRead = async (message) => {
  if (!message.unread) return;

  try {
    // 调用接口标记为已读
    await request.put(`/api/notifications/${message.id}/read`);
    
    // 更新本地状态
    message.unread = false;
  } catch (err) {
    console.error('标记消息为已读失败:', err);
    toast.error('更新消息状态失败');
  }
};

/**
 * 格式化时间显示
 */
const formatTime = (timeStr) => {
  if (!timeStr) return '';
  
  const date = new Date(timeStr);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};
</script>

<style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.3/css/all.min.css');

.notifications-container {
  max-width: 600px;
  margin: 0 auto;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  color: #333;
  min-height: 100vh;
  background-color: #fff;
}

.header {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 50px;
  background-color: #0097ff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  z-index: 100;
}
/* .header {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid #e0e0e0;
  position: relative;
} */

.title {
 font-size: 1.1rem;
  color: #ffffff;
  font-weight: 600;
  margin: 0;
}

.unread-badge {
  position: absolute;
  top: 15px;
  right: 20px;
  background-color: #ff4d4f;
  color: white;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
}

.notification-list {
  padding: 0;
  margin: 15vw;
}

.notification-item {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s ease;
  cursor: pointer;
}

.notification-item:hover {
  background-color: #f5f8ff; /* 浅蓝色背景 */
}

.notification-item:last-child {
  border-bottom: none;
}

.icon-wrapper {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 15px;
  background-color: #e6f0ff; /* 浅蓝色 */
}

.icon-wrapper.unread {
  background-color: #c9e2ff; /* 针对未读消息的更深的蓝色 */
}

.icon {
  width: 20px;
  height: 20px;
  color: #1a73e8; /* 图标蓝色 */
}

.content {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.message-text {
  font-size: 16px;
  line-height: 1.5;
  color: #555;
}

.message-text.bold {
  font-weight: 600;
  color: #1a73e8; /* 未读消息文本蓝色 */
}

.timestamp {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}

.dot {
  width: 8px;
  height: 8px;
  background-color: #ff4d4f; /* 未读小红点 */
  border-radius: 50%;
  margin-left: 10px;
  flex-shrink: 0;
}

/* 空状态样式 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #999;
}

.empty-state i {
  font-size: 3rem;
  margin-bottom: 15px;
  color: #ddd;
}

.empty-state p {
  font-size: 1.1rem;
}

/* 加载状态样式 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #666;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #1a73e8;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 错误状态样式 */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #f5222d;
  text-align: center;
}

.error-state i {
  font-size: 3rem;
  margin-bottom: 15px;
}

.error-state p {
  font-size: 1.1rem;
  margin-bottom: 20px;
}

.retry-btn {
  background-color: #1a73e8;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.retry-btn:hover {
  background-color: #0d66d0;
}
</style>
