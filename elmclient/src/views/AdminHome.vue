<template>
  <div class="admin-container">
    <div class="container">
      <div class="top-background">
        <h1>管理员管理平台</h1>
      </div>

      <div class="user-card">
        <div class="avatar">
          <img src="@/assets/default-avatar.png" alt="用户头像" class="avatar-img" />
        </div>
        <div class="user-details">
          <div class="user-name">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512">
              <path fill="currentColor" d="M224 256c70.7 0 128-57.31 128-128s-57.3-128-128-128S96 57.31 96 128s57.3 128 128 128zm-45.72 22.78c-55.8 17.5-91.1 72.8-91.1 133.3c0 6.64 5.36 12 12 12h264c6.64 0 12-5.36 12-12c0-60.5-35.3-115.8-91.1-133.3C306.6 304.8 266.3 320 224 320s-82.55-15.2-96.28-41.22z" />
            </svg>
            <span>{{ currentUser?.username || '管理员' }}</span>
          </div>
          <div class="user-full-name">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 576 512">
              <path fill="currentColor" d="M0 32C0 14.33 14.33 0 32 0h512c17.67 0 32 14.33 32 32v448c0 17.67-14.33 32-32 32h-512c-17.67 0-32-14.33-32-32V32zM320 128c0-17.67-14.33-32-32-32h-96c-17.67 0-32 14.33-32 32s14.33 32 32 32h96c17.67 0 32-14.33 32-32zm64 256c0 8.836-7.164 16-16 16h-288c-8.836 0-16-7.164-16-16s7.164-16 16-16h288c8.836 0 16 7.164 16 16zm0-96c0 8.836-7.164 16-16 16h-288c-8.836 0-16-7.164-16-16s7.164-16 16-16h288c8.836 0 16 7.164 16 16z" />
            </svg>
            <span>ID: ADMIN{{ currentUser?.id || '00' }}</span>
          </div>
        </div>
      </div>
      
      <!-- 新增的退出按钮 -->
      <button class="logout-btn" @click="logout">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">
          <path fill="currentColor" d="M384 320c-17.67 0-32 14.33-32 32v96c0 17.67-14.33 32-32 32H64c-17.67 0-32-14.33-32-32V128c0-17.67 14.33-32 32-32h256c17.67 0 32-14.33 32-32S337.7 0 320 0H64C28.65 0 0 28.65 0 64v384c0 35.35 28.65 64 64 64h256c35.35 0 64-28.65 64-64v-96c0-17.67-14.33-32-32-32zm48.51-140.51l-104.5-104.5c-4.688-4.688-12.29-4.688-16.97 0s-4.688 12.29 0 16.97L404.7 192H192c-17.67 0-32 14.33-32 32s14.33 32 32 32h212.7l-43.5 43.5c-4.688 4.688-4.688 12.29 0 16.97s12.29 4.688 16.97 0l104.5-104.5c4.688-4.688 4.688-12.29 0-16.97z" />
        </svg>
        退出登录
      </button>

      <div class="stats-toolbar">
        <label>统计区间</label><input v-model="statsFrom" type="date"><span>至</span><input v-model="statsTo" type="date">
        <button @click="loadDetailedStats">查询</button><button class="export-btn" @click="downloadStats">导出CSV</button>
        <span v-if="detailedStats" class="stats-summary">有效订单 {{ detailedStats.completedCount || 0 }} · 有效营业额 ¥{{ detailedStats.revenue || 0 }}</span>
      </div>
      <div class="stats-container">
        <div class="stat-card users">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 512">
            <path fill="currentColor" d="M224 256c70.7 0 128-57.31 128-128s-57.3-128-128-128S96 57.31 96 128s57.3 128 128 128zm-45.72 22.78c-55.8 17.5-91.1 72.8-91.1 133.3c0 6.64 5.36 12 12 12h264c6.64 0 12-5.36 12-12c0-60.5-35.3-115.8-91.1-133.3C306.6 304.8 266.3 320 224 320s-82.55-15.2-96.28-41.22zM480 320c44.11 0 80-35.89 80-80s-35.89-80-80-80s-80 35.89-80 80s35.89 80 80 80zm-44.57 33.39c-29.42-10.32-52.92-26.6-70.2-46.12c-2.31 29.56-11.23 58.73-28.71 85.34c-13.88 20.82-35.39 37.81-60.85 47.78c1.375 1.5 2.768 2.977 4.225 4.434c32.74 32.74 76.5 50.88 122.9 50.88c53.07 0 102.6-21.41 139.6-58.42c37.01-37.01 58.42-86.54 58.42-139.6c0-46.4-18.14-90.16-50.88-122.9C611.3 331.4 567.5 349.6 521.1 349.6c-17.7 0-35.03-3.951-51.48-11.53z" />
          </svg>
          <h3>总用户人数</h3>
          <div class="number">
            {{ userCount !== null ? userCount.toLocaleString() : '加载中...' }}
          </div>
        </div>

        <div class="stat-card shops">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 576 512">
            <path fill="currentColor" d="M576 432c0 26.51-21.49 48-48 48H48c-26.51 0-48-21.49-48-48V192c0-26.51 21.49-48 48-48h96v-16c0-52.93 43.07-96 96-96h96c52.93 0 96 43.07 96 96v16h96c26.51 0 48 21.49 48 48v240zM352 168c0-17.67-14.33-32-32-32h-96c-17.67 0-32 14.33-32 32v8h160v-8z" />
          </svg>
          <h3>总店铺数</h3>
          <div class="number">
            {{ shopCount !== null ? shopCount.toLocaleString() : '加载中...' }}
          </div>
        </div>

        <div class="stat-card revenue">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512">
            <path fill="currentColor" d="M384 480c-26.51 0-48 21.49-48 48H48c-26.51 0-48-21.49-48-48V320c0-26.51 21.49-48 48-48h288c26.51 0 48 21.49 48 48v160zM416 160c-26.51 0-48 21.49-48 48v112c0 26.51 21.49 48 48 48s48-21.49 48-48V208c0-26.51-21.49-48-48-48zm-64-32c-26.51 0-48 21.49-48 48s21.49 48 48 48s48-21.49 48-48s-21.49-48-48-48z" />
          </svg>
          <h3>总营业额</h3>
          <div class="number">
            ¥{{ totalRevenue !== null ? totalRevenue.toLocaleString() : '0' }}
          </div>
        </div>
      </div>

      <div class="review-section">
        <div class="review-tabs">
          <div class="review-tab" :class="{ active: activeTab === 'user-review' }" @click="activeTab = 'user-review'">
            用户审核（{{ merchantApplications.length }}）
          </div>
          <div class="review-tab" :class="{ active: activeTab === 'shop-review' }" @click="activeTab = 'shop-review'">
            商铺审核（{{ shopApplications.length }}）
          </div>
        </div>

        <div class="review-content" :class="{ active: activeTab === 'user-review' }">
          <div v-if="merchantApplications.length === 0 && !loadingMerchant" class="empty-tip">
            暂无待审核的商家申请
          </div>
          <div v-if="loadingMerchant" class="loading-tip">加载中...</div>
          <div v-for="app in merchantApplications" :key="'merchant-' + app.id" class="review-item">
            <div class="review-info">
              <h3>用户ID: {{ app.userId }}</h3>
              <p>用户名: {{ app.username }}</p>
              <p>申请时间: {{ formatTime(app.createTime) }}</p>
            </div>
            <button class="review-btn" @click="handleMerchantReview(app)">审核</button>
          </div>
        </div>

        <div class="review-content" :class="{ active: activeTab === 'shop-review' }">
          <div v-if="shopApplications.length === 0 && !loadingShop" class="empty-tip">
            暂无待审核的店铺申请
          </div>
          <div v-if="loadingShop" class="loading-tip">加载中...</div>
          <div v-for="app in shopApplications" :key="'shop-' + app.id" class="review-item">
            <div class="review-info">
              <h3>{{ app.businessName }}</h3>
              <p>店铺类型: {{ getShopTypeName(app.orderTypeId) }}</p>
              <p>申请时间: {{ formatTime(app.createTime) }}</p>
            </div>
            <button class="review-btn" @click="handleShopReview(app)">审核</button>
          </div>
        </div>
      </div>

      <div v-if="showReviewModal" class="modal-overlay" @click.self="closeModal">
        <div class="modal-content">
          <div class="modal-header">
            <h3>{{ modalTitle }}</h3>
            <span class="close-btn" @click="closeModal">&times;</span>
          </div>
          <div class="modal-body">
            <div v-if="modalType === 'merchant'">
              <div v-if="loadingPersonInfo" class="loading-tip">加载用户信息中...</div>

              <div v-else-if="currentPersonInfo" class="person-info-container">
                <div class="modal-item">
                  <label>用户名:</label>
                  <span>{{ currentMerchantApp?.username }}</span>
                </div>
                <div class="modal-item">
                  <label>姓名:</label>
                  <span>{{ currentPersonInfo.firstName }} {{ currentPersonInfo.lastName }}</span>
                </div>
                <div class="modal-item">
                  <label>手机号:</label>
                  <span>{{ currentPersonInfo.phone || '未填写' }}</span>
                </div>
                <div class="modal-item">
                  <label>邮箱:</label>
                  <span>{{ currentPersonInfo.email || '未填写' }}</span>
                </div>
                <div class="modal-item">
                  <label>性别:</label>
                  <span>
                    {{
                      currentPersonInfo.gender === 'MALE'
                        ? '男'
                        : currentPersonInfo.gender === 'FEMALE'
                        ? '女'
                        : '未设置'
                    }}
                  </span>
                </div>
                <div class="modal-item">
                  <label>申请ID:</label>
                  <span>{{ currentMerchantApp?.id }}</span>
                </div>
                <div class="modal-item">
                  <label>申请时间:</label>
                  <span>{{ formatTime(currentMerchantApp?.createTime) }}</span>
                </div>
              </div>

              <div v-else class="empty-tip">无法获取用户详细信息</div>
            </div>

            <div v-if="modalType === 'shop'">
              <div class="modal-item">
                <label>申请ID:</label>
                <span>{{ currentShopApp?.id }}</span>
              </div>
              <div class="modal-item">
                <label>店铺名称:</label>
                <span>{{ currentShopApp?.businessName }}</span>
              </div>
              <div class="modal-item">
                <label>店铺地址:</label>
                <span>{{ currentShopApp?.businessAddress }}</span>
              </div>
              <div class="modal-item">
                <label>店铺介绍:</label>
                <span>{{ currentShopApp?.businessExplain }}</span>
              </div>
              <div class="modal-item">
                <label>起送价:</label>
                <span>¥{{ currentShopApp?.startPrice.toFixed(2) }}</span>
              </div>
              <div class="modal-item">
                <label>配送费:</label>
                <span>¥{{ currentShopApp?.deliveryPrice.toFixed(2) }}</span>
              </div>
              <div class="modal-item">
                <label>店铺类型:</label>
                <span>{{ getShopTypeName(currentShopApp?.orderTypeId) }}</span>
              </div>
              <div class="modal-item">
                <label>申请时间:</label>
                <span>{{ formatTime(currentShopApp?.createTime) }}</span>
              </div>
              <div class="modal-item" v-if="currentShopApp?.businessImg">
                <label>店铺图片:</label>
                <img :src="currentShopApp?.businessImg || require('@/assets/business-default.png')" @error="handleImageError" alt="店铺图片" class="shop-img" />
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="modal-btn approve-btn" @click="submitAudit(1)">批准</button>
            <button class="modal-btn reject-btn" @click="submitAudit(2)">拒绝</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, reactive } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';
import { toast } from '../utils/toast';
import { getWebSocketUrl } from '@/utils/endpoints';
import { clearAuth, getStoredUser, updateStoredUser } from '@/utils/auth';

// 路由实例
const router = useRouter();

// 状态管理
const currentUser = ref({}); // 当前管理员信息
const userCount = ref(null); // 总用户数
const shopCount = ref(null); // 总店铺数
const totalRevenue = ref(null); // 总营业额
const statsFrom = ref('');
const statsTo = ref('');
const detailedStats = ref(null);
const activeTab = ref('user-review'); // 活跃的审核标签
const showReviewModal = ref(false); // 审核弹窗显示状态
const modalTitle = ref(''); // 弹窗标题
const modalType = ref(''); // 弹窗类型（merchant/shop）
const currentMerchantApp = ref({}); // 当前待审核的商家申请
const currentShopApp = ref(null); // 当前待审核的店铺申请
const loadingMerchant = ref(false); // 商家申请加载状态
const loadingShop = ref(false); // 店铺申请加载状态

// 申请列表数据
const merchantApplications = ref([]); // 商家申请列表
const shopApplications = ref([]); // 店铺申请列表

// WebSocket实例
const webSocket = ref(null);
const currentPersonInfo = ref(null); // 存储用户详细信息
const loadingPersonInfo = ref(false); // 加载状态

const handleImageError = (event) => {
  const image = event?.target;
  if (!image || image.dataset.fallbackApplied === 'true') return;
  image.dataset.fallbackApplied = 'true';
  image.src = require('@/assets/business-default.png');
};

// 店铺类型映射（根据实际业务补充）
const shopTypeMap = computed(() => ({
  1: '美食',
  2: '早餐',
  3: '跑腿代购',
  4: '汉堡披萨',
  5: '甜品饮品',
  6: '速食简餐',
  7: '地方小吃',
  8: '米粉面馆',
  9: '包子粥铺',
  10: '炸鸡炸串',
}));

// ====================== 初始化逻辑 ======================
onMounted(() => {
  // 1. 获取当前管理员信息
  getCurrentUserInfo();
  // 2. 获取统计数据
  getStatisticData();
  loadDetailedStats();
  // 3. 获取待审核列表
  getMerchantApplications();
  getShopApplications();
  // 4. 初始化WebSocket
  initWebSocket();
});

// 销毁WebSocket连接
onUnmounted(() => {
  if (webSocket.value) {
    webSocket.value.close();
  }
});

// ====================== 接口请求 ======================
/**
 * 获取当前管理员信息
 */
const getCurrentUserInfo = async () => {
  try {
    const savedUser = getStoredUser();
    if (savedUser) {
      currentUser.value = savedUser;
    } else {
      const res = await request.get('/api/user');
      const user = res?.success ? res.data : res;
      if (user?.id) {
        currentUser.value = user;
        updateStoredUser(user);
      }
    }
  } catch (error) {
    console.error('获取管理员信息失败:', error);
    toast.error('获取管理员信息失败，请刷新重试');
  }
};

const getPersonInfo = async (userId) => {
  if (!userId) return;

  loadingPersonInfo.value = true;
  try {
    // 调用接口，传入用户ID作为query参数
    const res = await request.get('/api/personInfo', {
      params: { id: userId },
    });

    if (res.success && res.data) {
      currentPersonInfo.value = res.data;
    } else {
      toast.warning('获取用户信息失败');
      currentPersonInfo.value = null;
    }
  } catch (error) {
    console.error('获取用户详细信息失败:', error);
    toast.error('获取用户信息失败，请重试');
    currentPersonInfo.value = null;
  } finally {
    loadingPersonInfo.value = false;
  }
};

/**
 * 获取统计数据（用户数、店铺数、营业额）
 */
const getStatisticData = async () => {
  try {
    // 并行请求统计接口
    const [userRes, shopRes, revenueRes] = await Promise.all([
      request.get('/api/admin/countUser'),
      request.get('/api/admin/countBusiness'),
      request.get('/api/admin/countPrice'),
    ]);

    if (userRes.success) userCount.value = userRes.data;
    if (shopRes.success) shopCount.value = shopRes.data;
    if (revenueRes.success) totalRevenue.value = revenueRes.data;
  } catch (error) {
    console.error('获取统计数据失败:', error);
    toast.error('获取统计数据失败');
  }
};

const loadDetailedStats = async () => {
  try { const res = await request.get('/api/admin/statistics', { params: { from: statsFrom.value || undefined, to: statsTo.value || undefined } }); if (res.success) detailedStats.value = res.data; } catch (e) { toast.error('统计数据加载失败'); }
};
const downloadStats = async () => {
  try { const blob = await request.get('/api/admin/statistics/export', { params: { from: statsFrom.value || undefined, to: statsTo.value || undefined }, responseType: 'blob' }); const url=URL.createObjectURL(blob); const a=document.createElement('a'); a.href=url; a.download='statistics.csv'; a.click(); URL.revokeObjectURL(url); } catch (e) { toast.error('导出失败'); }
};

/**
 * 获取商家申请待审核列表
 */
const getMerchantApplications = async () => {
  loadingMerchant.value = true;
  try {
    const res = await request.get('/api/permission/merchant-applications');
    if (res.success && Array.isArray(res.data)) {
      // 补充申请时间（若接口未返回，可根据实际调整）
      console.log("第一个申请列表后端:",res.data);
      merchantApplications.value = res.data.map((app) => ({
        ...app,
        createTime: app.createTime || new Date().toISOString(),
      }));
      console.log("第一个申请列表后端:",merchantApplications.value);
    }
  } catch (error) {
    console.error('获取商家申请列表失败:', error);
    toast.error('获取商家申请列表失败');
  } finally {
    loadingMerchant.value = false;
  }
};

/**
 * 获取店铺申请待审核列表
 */
const getShopApplications = async () => {
  loadingShop.value = true;
  try {
    const res = await request.get('/api/permission/shop-applications');
    if (res.success && Array.isArray(res.data)) {
      shopApplications.value = res.data;
    }
  } catch (error) {
    console.error('获取店铺申请列表失败:', error);
    toast.error('获取店铺申请列表失败');
  } finally {
    loadingShop.value = false;
  }
};

/**
 * 提交商家审核结果
 */
const submitMerchantAudit = async (id, auditResult) => {
  try {
    const res = await request.post('/api/permission/audit', {
      id,
      auditResult, // 1-批准，2-拒绝
    });
    if (res.success) {
      toast.success(auditResult === 1 ? '批准成功' : '拒绝成功');
      // 重新获取申请列表
      getMerchantApplications();
      closeModal();
    } else {
      toast.error(res.message || '审核提交失败');
    }
  } catch (error) {
    console.error('提交商家审核失败:', error);
    toast.error('提交审核失败，请重试');
  }
};

/**
 * 提交店铺审核结果
 */
const submitShopAudit = async (id, auditResult) => {
  try {
    const res = await request.post('/api/permission/audit-shop', {
      id,
      status: auditResult, // 1-批准，2-拒绝（与接口字段匹配）
    });
    if (res.success) {
      toast.success(auditResult === 1 ? '批准成功' : '拒绝成功');
      // 重新获取申请列表
      getShopApplications();
      closeModal();
    } else {
      toast.error(res.message || '审核提交失败');
    }
  } catch (error) {
    console.error('提交店铺审核失败:', error);
    toast.error('提交审核失败，请重试');
  }
};

// ====================== WebSocket相关 ======================
/**
 * 初始化WebSocket连接
 */
const initWebSocket = () => {
  // 1. 生成唯一的客户端标识sid（可以使用时间戳+随机数）
  const sid = `admin-${Date.now()}-${Math.floor(Math.random() * 1000)}`;

  // 2. 构造WebSocket连接地址（匹配后端的/ws/{sid}端点）
  const wsUrl = getWebSocketUrl(`/ws/${sid}`);

  webSocket.value = new WebSocket(wsUrl);

  // 3. 连接成功
  webSocket.value.onopen = () => {
    console.log(`WebSocket连接成功，客户端标识：${sid}`);
    // 可以在这里存储sid，用于后续可能的单独消息发送
    sessionStorage.setItem('websocket_sid', sid);
  };

  // 4. 接收消息（保持不变）
  webSocket.value.onmessage = (event) => {
    try {
      const message = JSON.parse(event.data);
      handleWebSocketMessage(message);
    } catch (error) {
      console.error('解析WebSocket消息失败:', error);
    }
  };

  // 5. 连接关闭（保持不变，增加重连逻辑）
  webSocket.value.onclose = (event) => {
    console.log(`WebSocket连接关闭（sid: ${sid}），正在重连...`, event);
    // 延迟重连，避免频繁尝试
    setTimeout(initWebSocket, 3000);
  };

  // 6. 连接错误（保持不变）
  webSocket.value.onerror = (error) => {
    console.error(`WebSocket连接错误（sid: ${sid}）:`, error);
  };
};

/**
 * 处理WebSocket消息
 */
const handleWebSocketMessage = (message) => {
  const { applicationId, type, userId, username, content } = message;
  // 提示消息
  toast.info(content);

  // 1. 商家申请通知（type=0）
  if (type === 0) {
    getMerchantApplications();
  }

  // 2. 店铺申请通知（根据后端实际type值调整，这里假设为1）
  if (type === 1) {
    // 触发重新获取列表或直接添加
    getShopApplications();
  }
};

// ====================== 事件处理 ======================
/**
 * 处理商家审核点击
 */
const handleMerchantReview = async (app) => {
  modalType.value = 'merchant';
  modalTitle.value = `商家申请审核 - ${app.username}`;
  //currentMerchantApp.value = app;
  currentMerchantApp.value = { ...app };

  console.log('currentMerchantApp.value.id:', currentMerchantApp.value.id);
  // 打开弹窗前先获取用户详细信息
  await getPersonInfo(app.userId);
  showReviewModal.value = true;
};

/**
 * 处理店铺审核点击
 */
const handleShopReview = (app) => {
  modalType.value = 'shop';
  modalTitle.value = `店铺申请审核 - ${app.businessName}`;
  currentShopApp.value = app;
  showReviewModal.value = true;
};

/**
 * 提交审核结果
 */
const submitAudit = (result) => {
  if (modalType.value === 'merchant' && currentMerchantApp.value) {
    submitMerchantAudit(currentMerchantApp.value.id, result);
  }
  if (modalType.value === 'shop' && currentShopApp.value) {
    submitShopAudit(currentShopApp.value.id, result);
  }
};

/**
 * 关闭审核弹窗
 */
const closeModal = () => {
  showReviewModal.value = false;
  currentMerchantApp.value = null;
  currentShopApp.value = null;
  modalType.value = '';
  modalTitle.value = '';
};

/**
 * 退出登录
 */
const logout = () => {
  clearAuth();
  // 关闭WebSocket连接
  if (webSocket.value) {
    webSocket.value.close();
  }
  // 跳转到登录页
  router.push('/index');
  toast.success('已成功退出登录');
};

/**
 * 格式化时间
 */
const formatTime = (timeStr) => {
  if (!timeStr) return '';
  const date = new Date(timeStr);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

/**
 * 获取店铺类型名称
 */
const getShopTypeName = (typeId) => {
  return shopTypeMap.value[typeId] || `未知类型(${typeId})`;
};
</script>

<style scoped>
/* 保持所有原有样式不变 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: 'Helvetica Neue', Arial, sans-serif;
}

.admin-container {
  background-color: #ffffff;
  color: write;
  line-height: 1.6;
  padding: 0;
  max-width: 100%;
  overflow-x: hidden;
  padding-bottom: 0;
  /* 关键：允许垂直溢出滚动 */
  overflow-y: auto;
  /* 优化：设置最小高度为屏幕高度，确保内容少也占满屏幕 */
  min-height: 100vh;
}
.container {
  max-width: 600px;
  margin: 0 auto;
  background: #ffffff;
  /* min-height: 100vh; */
  /* box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08); */
  border-radius: 16px;
  padding-top: 150px;
  padding-bottom: 65px;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  height: 100%;
}
/* 确保底部导航在最上层 */
.admin-footer {
  z-index: 1000; /* 提高 z-index */
  padding-bottom: 0;
  background-color: transparent;
}
.admin-container {
  /* 确保容器有足够的内边距给底部导航留出空间 */
  padding-bottom: 0; /* 导航高度 + 一些间距 */
}
.top-background {
  width: 100%;
  height: 100px;
  background: linear-gradient(to right, #3a7bd5, #00d2ff);
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-radius: 16px 16px 0 0;
  position: fixed;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  overflow: hidden;
  margin-bottom: 50px;
  max-width: 600px;
}
.top-background::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0) 70%);
  transform: rotate(30deg);
  animation: shine 6s infinite linear;
}
@keyframes shine {
  0% {
    transform: rotate(30deg) translate(-10%, -10%);
  }
  100% {
    transform: rotate(30deg) translate(10%, 10%);
  }
}
.top-background h1 {
  color: white;
  font-size: 1.8rem;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  letter-spacing: 1px;
  margin: 0;
  z-index: 1;
}

/* 移除原有的顶部退出按钮样式 */
.top-logout-btn {
  display: none;
}

/* 新增的退出按钮样式 */
.logout-btn {
  width: 92%;
  max-width: 500px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #2782dd, #61c8f4); /* 红色系，与主题和谐 */
  color: white;
  border: none;
  border-radius: 12px;
  padding: 12px 20px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 12px rgba(60, 140, 238, 0.3);
  transform: translateY(-40px);
  z-index: 2;
}
.logout-btn:hover {
  background-color: #0085e0;
  box-shadow: 0 6px 16px rgba(63, 95, 241, 0.4);
}
.logout-btn svg {
  color: white;
  width: 1.2em;
  height: 1.2em;
}

.user-card {
  width: 92%;
  max-width: 500px;
  margin: -50px auto 20px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  padding: 20px 0;
  display: flex;
  align-items: center;
  gap: 20px;
  position: relative;
  z-index: 2;
  /* transform: translateY(-50px); */
}
.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid white;
  box-shadow: 0 6px 20px rgba(0, 151, 255, 0.3);
  flex-shrink: 0;
  background: #f8f9fa;
  margin-left: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar svg {
  font-size: 2.5rem;
  color: #777;
}
.user-details {
  flex: 1;
  background-color: #f8f9fa;
  padding: 15px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid #e9ecef;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-right: 15px;
}
.user-name,
.user-full-name,
.user-phone,
.user-email {
  font-size: 0.95rem;
  color: #495057;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  padding: 10px;
  display: flex;
  align-items: center;
}
.user-name {
  font-size: 1.1rem;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

/* 为所有的 SVG 图标添加基础样式 */
.admin-container svg {
  width: 1.2em; /* 保持图标大小一致 */
  height: 1.2em;
  /* 确保垂直对齐 */
  vertical-align: -0.125em;
  /* 保持与原来图标的间距 */
  margin-right: 8px;
}

/* 调整用户卡片内 SVG 的样式 */
.user-details svg {
  margin-right: 8px;
  color: #3498db;
}

.user-full-name .first-name {
  margin-right: 5px;
}

.stats-container {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 25px;
  overflow-x: auto;
  padding-bottom: 5px;
  width: 92%;
  max-width: 500px;
  transform: translateY(-40px);
}
.stats-container::-webkit-scrollbar {
  display: none;
}
.stat-card {
  background: white;
  border-radius: 12px;
  padding: 15px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  flex: 1;
  min-width: 100px;
}

/* 调整统计卡片内 SVG 的颜色 */
.stat-card svg {
  font-size: 1.8rem;
  margin-bottom: 8px;
  margin-right: 0;
}
.stat-card.users svg {
  color: #0097ff;
}
.stat-card.shops svg {
  color: #ff6700;
}
.stat-card.revenue svg {
  color: #4caf50;
}
.stat-card h3 {
  font-size: 0.85rem;
  margin-bottom: 8px;
  color: #555;
  font-weight: 500;
}
.stat-card .number {
  font-size: 1.4rem;
  font-weight: 700;
  color: #333;
}
.review-section {
  background: white;
  border-radius: 16px;
  max-height: none;
  overflow: visible;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
  margin-bottom: 30px;
  width: 92%;
  max-width: 500px;
  transform: translateY(-40px);
}
.review-tabs {
  display: flex;
  border-bottom: 1px solid #f0f0f0;
}
.review-tab {
  flex: 1;
  text-align: center;
  padding: 15px 0;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #888;
}
.review-tab.active {
  color: #0097ff;
  border-bottom: 3px solid #0097ff;
}
.review-content {
  display: none;
  padding: 0;
}
.review-content.active {
  display: block;
}
.review-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #f0f0f0;
}
.review-item:last-child {
  border-bottom: none;
}
.review-info {
  flex: 1;
}
.review-info h3 {
  font-size: 1rem;
  margin-bottom: 5px;
  color: #333;
}
.review-info p {
  color: #888;
  font-size: 0.85rem;
}
.review-btn {
  background: #0097ff;
  color: white;
  border: none;
  border-radius: 20px;
  padding: 8px 15px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.3s ease;
}
.review-btn:hover {
  background: #0085e0;
}
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}
.modal-content {
  background: white;
  border-radius: 12px;
  padding: 20px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  gap: 15px;
  animation: fadeIn 0.3s ease-out;
}
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}
.modal-header h3 {
  margin: 0;
  font-size: 1.2rem;
  color: #333;
}
.close-btn {
  font-size: 1.5rem;
  color: #aaa;
  cursor: pointer;
  transition: color 0.2s;
}
.close-btn:hover {
  color: #666;
}
.modal-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.modal-item {
  display: flex;
  align-items: baseline;
  padding: 5px;
  border-radius: 8px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
}
.modal-item label {
  font-weight: bold;
  color: #555;
  margin-right: 10px;
  min-width: 80px;
}
.modal-item span {
  flex: 1;
  color: #333;
  word-break: break-all;
}
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 10px;
  border-top: 1px solid #eee;
}
.modal-btn {
  border: none;
  border-radius: 20px;
  padding: 10px 20px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  transition: all 0.3s ease;
}
.approve-btn {
  background-color: #0097ff;
  color: white;
}
.approve-btn:hover {
  background-color: #0085e0;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
}
.reject-btn {
  background-color: #e0e0e0;
  color: #333;
}
.reject-btn:hover {
  background-color: #c7c7c7;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
@media (max-width: 480px) {
  .container {
    max-width: 100vw;
    width: 100vw;
    border-radius: 0;
    padding-top: 150px;
    padding-bottom: 65px;
  }
  .user-card,
  .stats-container,
  .review-section {
    max-width: 100vw;
    width: 100vw;
    border-radius: 0;
    padding: 0;
  }
  .top-background {
    height: 90px;
    margin-bottom: 50px;
    border-radius: 0;
    max-width: 100vw;
  }
  .user-card {
    flex-direction: column;
    align-items: center;
    gap: 10px;
    padding: 20px 0;
    margin-top: 0;
    transform: translateY(-50px);
    width: 90%;
  }
  .avatar {
    width: 80px;
    height: 80px;
    margin-left: 0;
  }
  .user-details {
    width: 85%;
    padding: 10px;
    gap: 6px;
    margin-right: 0;
  }
  .stats-container,
  .review-section {
    width: 90%;
  }
  .modal-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
.empty-tip {
  text-align: center;
  padding: 20px;
  color: #666;
}

.loading-tip {
  text-align: center;
  padding: 20px;
  color: #666;
}

.shop-img {
  max-width: 200px;
  max-height: 150px;
  margin-top: 10px;
  border-radius: 4px;
}

.avatar {
  /* 假设头像框是固定大小，根据你的实际情况调整 */
  width: 80px;
  height: 80px;
  /* 可选：如果需要圆形头像框 */
  border-radius: 50%;
  /* 确保图片不会超出容器 */
  overflow: hidden;
  /* 可选：添加边框 */
  border: 2px solid #fff;
}

.avatar-img {
  /* 让图片填满容器 */
  width: 100%;
  height: 100%;
  /* 关键属性：控制图片如何适应容器 */
  object-fit: cover; /* 保持比例并裁剪以填满容器（常用） */
  /* 其他可选值：
    - contain: 保持比例，完整显示图片（可能有留白）
    - fill: 拉伸图片填满容器（可能变形）
    - none: 保持原始大小，可能显示不全
  */
  /* 可选：图片居中显示 */
  object-position: center;
}

/* 管理首页去掉装饰性渐变，保持蓝白工作台的统一密度 */
.admin-container { background: #f5f9fd; color: #24405c; }
.admin-container .container { background: #f5f9fd; border-radius: 0; box-shadow: none; padding-top: 88px; padding-bottom: 88px; }
.admin-container .top-background { height: 64px; background: #0097ff; background-image: none; border-radius: 0; box-shadow: 0 1px 0 rgba(0,83,145,.15); }
.admin-container .top-background::before { display: none; }
.admin-container .top-background h1 { font-size: 20px; letter-spacing: 0; text-shadow: none; }
.admin-container .logout-btn { background: #0097ff; box-shadow: none; border-radius: 7px; transform: none; }
.admin-container .logout-btn:hover { background: #087dcc; box-shadow: none; }
@media (max-width: 480px) {
  .admin-container .container { padding-top: 78px; }
  .admin-container .top-background { height: 64px; }
}
</style>

<style scoped>
.stats-toolbar{display:flex;align-items:center;gap:8px;flex-wrap:wrap;width:calc(100% - 32px);max-width:568px;margin:16px auto 0;color:#668095;font-size:13px}.stats-toolbar input{border:1px solid #d7e5ef;border-radius:6px;padding:7px;color:#49677f;min-width:0;flex:1 1 120px}.stats-toolbar button{border:0;border-radius:6px;padding:7px 12px;background:#168bd1;color:#fff;cursor:pointer;flex:0 0 auto}.stats-toolbar .export-btn{background:#fff;color:#168bd1;border:1px solid #168bd1}.stats-summary{width:100%;margin-left:0;color:#3d6d8c}.stats-container{transform:none;margin-top:16px}
.stat-card.users svg,.stat-card.shops svg,.stat-card.revenue svg{color:#2b8bc8}
</style>
