<template>
  <div class="wrapper">
    <header class="order-page-header">
      <h1>订单</h1>
      <label class="order-search">
        <i class="fa fa-search"></i>
        <input v-model.trim="searchKeyword" type="search" placeholder="搜索商家或订单号">
        <button v-if="searchKeyword" type="button" aria-label="清空搜索" @click="searchKeyword = ''">×</button>
      </label>
    </header>

    <div class="fixed-header">
      <ul class="tabs">
        <li v-for="(t, idx) in tabs" :key="t" :class="{ active: activeTab === idx }" @click="changeTab(idx)">
          {{ t }} <span v-if="orderCounts[idx] > 0">({{ orderCounts[idx] }})</span>
        </li>
      </ul>
    </div>

    <!-- 内容区域 -->
    <div class="content-area">
      <!-- 加载提示 -->
      <div v-if="loading" class="loading">
        <p>加载中...</p>
      </div>

      <!-- 空状态提示 -->
      <div v-else-if="displayedOrders.length === 0" class="empty-state">
        <img src="../assets/empty-order.png" alt="暂无订单">
        <p>暂无订单</p>
      </div>

      <section v-for="group in groupedOrders" :key="group.key" class="month-group">
        <div class="month-summary"><strong>{{ group.label }}</strong><span>支出 ¥{{ group.total }}</span></div>
        <ul class="order-list">
          <li v-for="item in group.orders" :key="item.id" class="order-item" @click="goDetail(item.id)" title="查看详情">
            <div class="order-header">
              <span class="order-id">订单号 {{ item.id }}</span>
              <span class="status-badge" :class="getStatusClass(item.orderState)">{{ getStatusText(item.orderState, item) }}</span>
            </div>

            <div class="order-content">
              <img class="thumb" :src="item.businessImg || require('@/assets/default-business.png')" alt="商家图片" @error="handleImageError">
              <div class="meta">
                <p class="name">{{ item.businessName || '未知商家' }} <i class="fa fa-angle-right"></i></p>
                <p class="time">{{ formatTime(item.orderDate || item.createTime) }}</p>
                <span v-if="item.serviceMode === 'PICKUP'" class="service-chip">到店自取</span>
                <span v-else class="service-chip">外送</span>
              </div>
              <div class="order-price"><strong>¥{{ Number(item.orderTotal || 0).toFixed(2) }}</strong></div>
            </div>

            <div class="actions">
              <template v-if="item.orderState === ORDER_STATUS.WAITING_PAYMENT">
                <button class="cancel-btn" @click.stop="cancelOrder(item.id)">取消订单</button>
                <button class="pay-btn" @click.stop="payOrder(item.id)">立即支付</button>
              </template>
              <template v-else-if="item.orderState === ORDER_STATUS.WAITING_MERCHANT_ACCEPT">
                <button class="cancel-btn" @click.stop="cancelOrder(item.id)">取消订单</button>
              </template>
              <template v-else-if="item.orderState === ORDER_STATUS.DELIVERED || (item.orderState === ORDER_STATUS.WAITING_PICKUP && item.serviceMode === 'PICKUP')">
                <button class="confirm-btn" @click.stop="confirmOrder(item.id)">确认收货</button>
              </template>
              <template v-else-if="item.orderState === ORDER_STATUS.COMPLETED">
                <button class="detail-btn" @click.stop="goDetail(item.id)">订单详情</button>
                <button class="review-btn" @click.stop="reviewOrder(item.id)">评价</button>
              </template>
              <template v-else>
                <button class="detail-btn" @click.stop="goDetail(item.id)">查看详情</button>
              </template>
            </div>
          </li>
        </ul>
      </section>
    </div>

  </div>

  <!-- 确认收货弹窗 -->
  <div v-if="showConfirmFinishedModal" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content">
      <div class="modal-header">
        <h3>确认操作</h3>
        <span class="close-btn" @click="closeModal">&times;</span>
      </div>
      <div class="modal-body">
        <p>确定要确认收货吗？</p>
      </div>
      <div class="modal-footer">
        <button class="modal-btn confirm-btn" @click="confirmFinished">确认</button>
        <button class="modal-btn cancel-btn" @click="closeModal">取消</button>
      </div>
    </div>
  </div>

  <!-- 确认取消弹窗 -->
  <div v-if="showConfirmCanceledModal" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content">
      <div class="modal-header">
        <h3>确认操作</h3>
        <span class="close-btn" @click="closeModal">&times;</span>
      </div>
      <div class="modal-body">
        <p>确认要取消订单吗？</p>
      </div>
      <div class="modal-footer">
        <button class="modal-btn confirm-btn" @click="confirmCanceled">确认</button>
        <button class="modal-btn cancel-btn" @click="closeModal">取消</button>
      </div>
    </div>
  </div>
</template>
  
<script>
import { ref, onMounted, computed, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import request from "../utils/request";
import { toast } from '../utils/toast';
import { createRealtimeConnection } from '../services/realtimeService';
import { CUSTOMER_ORDER_GROUPS, ORDER_STATUS, isOrderCountedAsSpend, orderStatusClass, orderStatusText } from '../utils/orderPresentation';
import { formatDateTime } from '../utils/formatters';

export default {
  name: "OrderList",
  setup() {
    const orderArr = ref([]);
    const userInfo = ref({});
    const router = useRouter();
    const loading = ref(false);
    const searchKeyword = ref('');
    let realtimeConnection = null;

    // 标签定义 - 与API状态对应
    const tabs = ["全部", "待支付", "待商家", "配送中", "已完成", "已取消"];
    const activeTab = ref(0);
    const showConfirmFinishedModal = ref(false);
    const showConfirmCanceledModal = ref(false);
    const selectId = ref(0);

    // 标签对应的API状态值
    const tabStatusMap = {
      0: null,
      1: CUSTOMER_ORDER_GROUPS.waitingPayment,
      2: CUSTOMER_ORDER_GROUPS.waitingMerchant,
      3: CUSTOMER_ORDER_GROUPS.fulfilling,
      4: CUSTOMER_ORDER_GROUPS.completed,
      5: CUSTOMER_ORDER_GROUPS.cancelled
    };

    // 获取订单列表
    const fetchOrders = async (status = null, { silent = false } = {}) => {
      if (!silent) loading.value = true;
      try {
        const params = {};
        if (status !== null) {
          params.orderState = status;
        }

        const response = await request.get("/api/orders/list/user"
          + (status === null ? "" : ("?orderState=" + status)));

        if (response.success) {
          orderArr.value = response.data || [];
        } else {
          console.error('获取订单列表失败:', response.data.message);
          if (!silent) toast.error('获取订单列表失败: ' + response.data.message);
        }
      } catch (error) {
        console.error("请求订单列表失败:", error);
        if (!silent) toast.error("获取订单列表失败，请稍后重试！");
      } finally {
        if (!silent) loading.value = false;
      }
    };

    // 计算各状态订单数量 - 基于完整订单列表
    const orderCounts = computed(() => {
      // 初始化一个数组，长度与tabs一致，初始值为0
      const counts = new Array(tabs.length).fill(0);

      // 获取完整的订单列表（从后端API获取的所有订单）
      const allOrders = orderArr.value;

      // 遍历所有订单进行统计
      allOrders.forEach(order => {
        const state = order.orderState;

        // 全部订单数
        counts[0]++;

        // 根据订单状态，增加对应标签的计数
        if (CUSTOMER_ORDER_GROUPS.waitingPayment.includes(state)) counts[1]++;
        else if (CUSTOMER_ORDER_GROUPS.waitingMerchant.includes(state)) counts[2]++;
        else if (CUSTOMER_ORDER_GROUPS.fulfilling.includes(state)) counts[3]++;
        else if (CUSTOMER_ORDER_GROUPS.completed.includes(state)) counts[4]++;
        else if (CUSTOMER_ORDER_GROUPS.cancelled.includes(state)) counts[5]++;
      });

      return counts;
    });

    // 计算显示的订单 - 基于当前选中的标签
    const displayedOrders = computed(() => {
      const targetStatus = tabStatusMap[activeTab.value];
      const byStatus = activeTab.value === 0 ? orderArr.value : orderArr.value.filter(order => targetStatus.includes(order.orderState));
      const keyword = searchKeyword.value.toLowerCase();
      if (!keyword) return byStatus;
      return byStatus.filter(order => String(order.id).includes(keyword)
        || String(order.businessName || '').toLowerCase().includes(keyword)
        || getStatusText(order.orderState, order).includes(searchKeyword.value));
    });

    const groupedOrders = computed(() => {
      const groups = new Map();
      displayedOrders.value.forEach(order => {
        const date = new Date(order.orderDate || order.createTime);
        const valid = Number.isFinite(date.getTime());
        const key = valid ? `${date.getFullYear()}-${date.getMonth() + 1}` : 'unknown';
        if (!groups.has(key)) {
          groups.set(key, {
            key,
            label: valid ? `${date.getFullYear()}年${date.getMonth() + 1}月` : '日期未知',
            timestamp: valid ? new Date(date.getFullYear(), date.getMonth(), 1).getTime() : 0,
            orders: [],
            amount: 0
          });
        }
        const group = groups.get(key);
        group.orders.push(order);
        if (isOrderCountedAsSpend(order.orderState)) group.amount += Number(order.orderTotal || 0);
      });
      return [...groups.values()].sort((a, b) => b.timestamp - a.timestamp).map(group => ({
        ...group,
        total: group.amount.toFixed(2)
      }));
    });

    // 切换标签 - 只需要改变activeTab，displayedOrders会自动更新
    const changeTab = (index) => {
      activeTab.value = index;
      // 不再需要在这里调用fetchOrders，因为displayedOrders是计算属性
    };

    // 获取状态文本
    const getStatusText = (state, order = {}) => {
      return orderStatusText(state, order, 'customer');
    };

    // 获取状态样式类
    const getStatusClass = (state) => {
      return orderStatusClass(state);
    };

    // 格式化时间
    const formatTime = (timeString) => {
      return formatDateTime(timeString, '');
    };

    // 取消订单
    const cancelOrder = (id) => {
      selectId.value = id;
      showConfirmCanceledModal.value = true;
    };

    // 确认取消订单
    const confirmCanceled = async () => {
      if (selectId.value === 0) return;

      try {
      const response = await request.put('/api/orders/status', null, { params: {
        orderState: ORDER_STATUS.CANCELLED,
        orderId: selectId.value
      } });

        if (response.success) {
          toast.success("订单取消成功");
          // 重新加载订单
          fetchOrders();
        } else {
          toast.error("取消失败,请重试");
        }
      } catch (error) {
        toast.error("取消失败,请重试");
      } finally {
        closeModal();
      }
    };

    // 支付订单
    const payOrder = (orderId) => {
      router.push({ path: "/payment", query: { orderId } });
    };

    // 确认收货
    const confirmOrder = (id) => {
      selectId.value = id;
      showConfirmFinishedModal.value = true;
    };

    // 确认完成订单
    const confirmFinished = async () => {
      if (selectId.value === 0) return;

      try {
        const response = await request.post(`/api/v1/orders/${selectId.value}/confirm-receipt`);

        if (response.success) {
          toast.success("订单完成");
          // 重新加载订单
          fetchOrders();
        } else {
          toast.error("确认完成失败,请重试");
        }
      } catch (error) {
        toast.error("确认完成失败,请重试");
      } finally {
        closeModal();
      }
    };

    // 查看订单详情
    const goDetail = (id) => {
      router.push({
        path: '/ListDetail',
        query: { orderId: id }
      });
    };
    const reviewOrder = (id) => {
      router.push({ path: '/listDetail', query: { orderId: id, focus: 'review' } });
    };

    // 关闭弹窗
    const closeModal = () => {
      showConfirmFinishedModal.value = false;
      showConfirmCanceledModal.value = false;
      selectId.value = 0;
    };

    const handleImageError = (event) => {
      event.target.src = require('@/assets/default-business.png');
    };

    onMounted(() => {
      // 获取用户信息
      const userData = sessionStorage.getItem("userInfo") || localStorage.getItem("userInfo");
      userInfo.value = userData ? JSON.parse(userData) : null;

      if (!userInfo.value) {
        toast.error("用户未登录，请先登录！");
        router.push({ path: "/login" });
        return;
      }
      realtimeConnection = createRealtimeConnection({
        onMessage: () => fetchOrders(null, { silent: true }),
        onFallbackRefresh: () => fetchOrders(null, { silent: true })
      });
      realtimeConnection.start();
      // 初始加载全部订单
      fetchOrders();
    });

    onUnmounted(() => {
      realtimeConnection?.stop();
    });

    return {
      orderArr,
      userInfo,
      tabs,
      activeTab,
      displayedOrders,
      groupedOrders,
      searchKeyword,
      loading,
      changeTab,
      getStatusText,
      getStatusClass,
      formatTime,
      cancelOrder,
      payOrder,
      confirmOrder,
      goDetail,
      reviewOrder,
      orderCounts,
      confirmCanceled,
      confirmFinished,
      showConfirmFinishedModal,
      showConfirmCanceledModal,
      closeModal,
      handleImageError,
      ORDER_STATUS
    };
  }
};
</script>
  
<style scoped>
/****************** 容器与顶部 ******************/
.wrapper {
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  min-height: 100vh;
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

/****************** 固定标题和筛选栏 ******************/
.fixed-header {
  position: fixed;
  top: 100px; /* 在顶部背景下方 */
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 600px;
  z-index: 999;
  background: white;
}

.page-title {
  padding: 4vw;
  font-size: 4.5vw;
  color: #333;
  font-weight: bold;
  background: white;
}

/****************** 标签栏 ******************/
.tabs {
  display: flex;
  align-items: center;
  padding: 0 4vw;
  background: white;
  border-bottom: 1px solid #f0f0f0;
  overflow-x: auto;
  white-space: nowrap;
}

.tabs li {
  margin-right: 6vw;
  padding: 3vw 0;
  font-size: 3.8vw;
  color: #666;
  position: relative;
  cursor: pointer;
}

.tabs li.active {
  color: #409eff;
  font-weight: 600;
}

.tabs li.active::after {
  content: "";
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  height: 0.8vw;
  background: #409eff;
  border-radius: 0.4vw;
}

/****************** 内容区域 ******************/
.content-area {
  margin-top: calc(100px + 18vw); /* 顶部背景高度 + 固定标题和筛选栏高度 */
  padding: 0 4vw;
  margin-bottom: 15vw;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

/****************** 加载和空状态 ******************/
.loading,
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 10vw;
  font-size: 4vw;
  color: #999;
}

.empty-state {
  flex-direction: column;
}

.empty-state img {
  width: 30vw;
  height: 30vw;
  margin-bottom: 4vw;
  opacity: 0.5;
}

/****************** 订单列表 ******************/
.order-list {
  padding: 4vw 0;
  /* 调整内边距 */
}

.order-item {
  background: #fff;
  border-radius: 2vw;
  box-shadow: 0 1vw 2vw rgba(0, 0, 0, .05);
  padding: 4vw;
  margin-bottom: 4vw;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 3vw;
  border-bottom: 1px solid #f5f5f5;
  margin-bottom: 3vw;
}

.order-id {
  font-size: 3.6vw;
  color: #999;
}

.status-badge {
  padding: 1vw 2vw;
  border-radius: 1vw;
  font-size: 3.2vw;
  font-weight: 500;
  position: relative;
  z-index: 10;
  /* 确保在最上层 */
}

.status-badge.unpaid {
  background: #fff0f0;
  color: #ff4d4f;
}

.status-badge.pending {
  background: #e6f7ff;
  color: #1890ff;
}

.status-badge.accepted {
  background: #f6ffed;
  color: #52c41a;
}

.status-badge.done {
  background: #fdf4de;
  color: #ffa700;
}

.status-badge.canceled {
  background: #f9f9f9;
  color: #999;
}

.order-content {
  display: flex;
  align-items: center;
  margin-bottom: 4vw;
}

.thumb {
  width: 20vw;
  height: 20vw;
  object-fit: cover;
  border-radius: 1.2vw;
  margin-right: 3vw;
}

.meta {
  flex: 1;
}

.name {
  font-size: 4.2vw;
  color: #333;
  font-weight: 500;
  margin-bottom: 1vw;
}

.items,
.time {
  font-size: 3.4vw;
  color: #999;
  margin-bottom: 1vw;
}

.price {
  font-size: 4.5vw;
  color: #ff6b00;
  font-weight: bold;
  margin-top: 2vw;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 2vw;
}

.actions button {
  padding: 2vw 4vw;
  border-radius: 1.6vw;
  font-size: 3.6vw;
  cursor: pointer;
  border: none;
}

.cancel-btn {
  background: #fff;
  color: #666;
  border: 1px solid #ddd !important;
}

.pay-btn {
  background: #409eff;
  color: #fff;
}

.confirm-btn {
  background: #52c41a;
  color: #fff;
}

.detail-btn {
  background: #f5f5f5;
  color: #666;
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
  from { opacity: 0; transform: scale(0.95) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
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

.modal-body p {
  color: #555;
  line-height: 1.5;
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

.cancel-btn {
  background-color: #e0e0e0;
  color: #333;
}

.cancel-btn:hover {
  background-color: #c7c7c7;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.confirm-btn {
  background-color: #1e80ff;
  color: white;
}

.confirm-btn:hover {
  background-color: #0085e0;
  box-shadow: 0 4px 12px rgba(30, 128, 255, 0.3);
}

@media (max-width: 480px) {
  .wrapper {
    max-width: 100vw;
    width: 100vw;
  }
  
  .top-background {
    height: 90px;
    border-radius: 0;
    max-width: 100vw;
  }
  
  .fixed-header {
    top: 90px;
    max-width: 100vw;
    transform: none;
    left: 0;
  }
  
  .content-area {
    margin-top: calc(130px + 18vw);
    max-width: 100vw;
    width: 100vw;
  }
}

.wrapper { width:100%; max-width:600px; margin:0 auto; background:#f4f7fa; color:#24405c; overflow-x:hidden; }
.order-page-header { position:fixed; z-index:1000; top:0; left:50%; transform:translateX(-50%); width:100%; max-width:600px; height:108px; box-sizing:border-box; padding:12px 16px; background:#fff; border-bottom:1px solid #e7eef4; }
.order-page-header h1 { margin:0 0 10px; color:#22384c; font-size:20px; line-height:28px; text-align:center; font-weight:650; }
.order-search { height:38px; display:flex; align-items:center; gap:8px; padding:0 12px; border-radius:8px; background:#f1f5f8; color:#8a9baa; }
.order-search input { flex:1; min-width:0; border:0; outline:0; background:transparent; color:#263c4e; font-size:14px; }
.order-search button { width:24px; height:24px; border:0; border-radius:50%; background:#d9e3ea; color:#667b8b; font-size:17px; line-height:20px; cursor:pointer; }
.fixed-header { top:108px; height:45px; left:50%; transform:translateX(-50%); max-width:600px; border-bottom:1px solid #edf2f6; }
.tabs { height:45px; padding:0 16px; justify-content:space-between; scrollbar-width:none; }
.tabs::-webkit-scrollbar{display:none}
.tabs li { flex:0 0 auto; margin:0; padding:13px 4px 10px; font-size:13px; }
.tabs li.active::after{height:3px;background:#0097ff;border-radius:3px}
.content-area { margin-top:153px; padding:0 0 88px; box-sizing:border-box; }
.month-group{margin-bottom:10px;background:#fff;border-top:1px solid #e7eef3;border-bottom:1px solid #e7eef3}
.month-summary{height:46px;padding:0 16px;display:flex;align-items:center;justify-content:space-between;background:#f7f9fb;color:#667b8b;font-size:13px}
.month-summary strong{color:#2c4255;font-size:15px;font-weight:600}
.order-list{padding:0;}
.order-item { border:0; border-radius:0; box-shadow:none; padding:14px 16px; margin:0; border-bottom:1px solid #edf2f5; }
.order-item:last-child{border-bottom:0}
.order-header{margin:0 0 12px;padding:0;border:0}
.order-id{font-size:12px;color:#93a2ae}
.status-badge{padding:0;background:transparent!important;font-size:13px}
.order-content{gap:12px;min-width:0;align-items:flex-start}
.order-content .thumb{width:62px;height:62px;flex:0 0 62px;margin:0;border-radius:7px;object-fit:cover}
.order-content .meta{min-width:0;flex:1}
.order-content .name,.order-content .time{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.order-content .name{margin:1px 0 7px;font-size:15px;color:#22384c;font-weight:600}
.order-content .name i{font-size:12px;color:#a3b0ba}
.order-content .time{margin:0 0 7px;font-size:12px;color:#8a9aa7}
.service-chip{display:inline-block;padding:2px 6px;border:1px solid #b9dff5;border-radius:3px;color:#2587bf;font-size:11px;line-height:16px}
.order-price{align-self:center;white-space:nowrap;color:#202d3d;font-size:15px}
.actions{margin-top:12px;padding-top:12px;border-top:1px solid #edf3f7;gap:8px}
.actions button{min-width:78px;padding:7px 13px;border-radius:6px;font-size:13px;background:#fff}
.pay-btn,.confirm-btn,.review-btn{background:#0097ff!important;color:#fff;border:1px solid #0097ff!important}
.detail-btn,.cancel-btn{background:#fff!important;color:#4d6171;border:1px solid #d8e2e9!important}
.order-item p,.order-item span{overflow-wrap:anywhere}
.loading,.empty-state{min-height:220px;font-size:14px}
@media (max-width:480px) {
  .order-page-header{left:0;transform:none;max-width:100vw}
  .fixed-header{top:108px;left:0;transform:none;max-width:100vw}
  .content-area{margin-top:153px;width:100%;max-width:none;padding-bottom:88px}
  .tabs{padding:0 10px}
  .tabs li{font-size:12px;padding-left:2px;padding-right:2px}
}
</style>
