import request from '../utils/request';

const responseData = (response) => {
  if (response?.success === false) throw new Error(response.message || '请求失败');
  return response?.data ?? response;
};

const errorText = (error) => error?.response?.data?.message || error?.message || '请求失败';

class AiChatService {
  async sendMessage(message, chatType = 'general', sessionId = null) {
    try {
      const data = responseData(await request.post('/api/ai/chat', {
        message: message.trim(),
        chatType,
        sessionId
      }));
      if (!data?.message) throw new Error('AI客服返回内容为空');
      return { success: true, data };
    } catch (error) {
      const networkFailure = error?.code === 'ERR_NETWORK' || error?.message === 'Network Error';
      const message = networkFailure
        ? '抱歉，网络连接失败，请检查网络后重试。'
        : '抱歉，AI客服暂时不可用，请稍后再试。';
      return {
        success: false,
        error: errorText(error),
        data: {
          message,
          sessionId: sessionId || this.generateSessionId(),
          responseType: 'error',
          responseTime: new Date().toISOString(),
          processingTime: 0
        }
      };
    }
  }

  async getChatHistory(page = 1, size = 20) {
    return this.loadHistory('/api/ai/chat/history', { page, size });
  }

  async getChatHistoryBySession(sessionId) {
    return this.loadHistory(`/api/ai/chat/history/session/${encodeURIComponent(sessionId)}`);
  }

  async loadHistory(url, params) {
    try {
      const data = responseData(await request.get(url, params ? { params } : undefined));
      return { success: true, data: Array.isArray(data) ? data : [] };
    } catch (error) {
      return { success: false, error: errorText(error), data: [] };
    }
  }

  async deleteChatHistory(historyId) {
    return this.runMutation(() => request.delete(`/api/ai/chat/history/${historyId}`));
  }

  async runMutation(action) {
    try {
      const data = responseData(await action());
      return { success: true, data: data ?? true };
    } catch (error) {
      return { success: false, error: errorText(error) };
    }
  }

  async healthCheck() {
    try {
      const message = responseData(await request.get('/api/ai/chat/health'));
      return { success: true, status: 'healthy', message };
    } catch (error) {
      return { success: false, status: 'unhealthy', error: errorText(error) };
    }
  }

  generateSessionId() {
    if (typeof window.crypto?.randomUUID === 'function') return window.crypto.randomUUID();
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (character) => {
      const random = Math.floor(Math.random() * 16);
      const value = character === 'x' ? random : (random & 0x3) | 0x8;
      return value.toString(16);
    });
  }

  formatTime(timestamp) {
    const date = new Date(timestamp);
    const now = new Date();
    const difference = now - date;
    const time = date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });

    if (difference < 24 * 60 * 60 * 1000 && now.getDate() === date.getDate()) return time;
    if (difference < 48 * 60 * 60 * 1000) return `昨天 ${time}`;
    return `${date.toLocaleDateString('zh-CN')} ${time}`;
  }

  detectChatType(message) {
    const normalized = message.toLowerCase();
    if (this.containsKeywords(normalized, ['商家', '店铺', '餐厅', '外卖店', '商户', '饭店', '推荐'])) {
      return 'business';
    }
    if (this.containsKeywords(normalized, ['菜', '菜品', '食物', '美食', '餐', '吃', '点餐', '菜单'])) {
      return 'food';
    }
    if (this.containsKeywords(normalized, ['订单', '下单', '支付', '配送', '外卖', '催单', '退款', '状态'])) {
      return 'order';
    }
    return 'general';
  }

  containsKeywords(message, keywords) {
    return keywords.some((keyword) => message.includes(keyword));
  }
}

export default new AiChatService();
