<template>
  <div class="wrapper">
    <!-- 顶部蓝色栏 -->
    <div class="top-background">
      <h1>商家订单管理</h1>
    </div>

    <!-- 商铺选择栏 -->
    <div class="merchant-selector">
      <ul class="merchant-tabs">
        <div class="merchant-no-business" v-if="merchantList.length === 0">您还没有商铺哦</div>
        <li
          v-for="merchant in merchantList"
          :key="merchant.merchantId"
          :class="{ active: selectedMerchantId === merchant.merchantId }"
          @click="selectMerchant(merchant.merchantId)"
        >
          {{ merchant.merchantName }}
        </li>
      </ul>
    </div>

    <!-- 订单状态分类 -->
    <ul class="tabs">
      <li
        v-for="(t, i) in statusTabs"
        :key="t"
        :class="{ active: activeStatusTab === i }"
        @click="changeStatusTab(i)"
      >
        {{ t }} <span v-if="orderCounts[i] > 0">({{ orderCounts[i] }})</span>
      </li>
    </ul>

    <!-- 加载提示 -->
    <div v-if="loading" class="loading">
      <p>加载中...</p>
    </div>

    <!-- 空状态提示 -->
    <div v-else-if="filteredOrders.length === 0" class="empty-state">
      <img src="../assets/empty-order.png" alt="暂无订单">
      <p>暂无订单</p>
    </div>

    <!-- 订单列表 -->
    <ul v-else class="order-list">
      <li v-for="order in filteredOrders" :key="order.id" class="order-item" title="查看订单详情">
        <div class="order-header">
          <span class="order-id">订单号: {{ order.id }}</span>
          <span class="status-badge" :class="getStatusClass(order.orderState)">{{ getStatusText(order.orderState, order) }}</span>
        </div>

        <div class="order-content">
          <div class="customer-info">
            <p><span>顾客:</span> {{ order.customerName || '-' }}</p>
            <template v-if="order.serviceMode !== 'PICKUP'">
              <p><span>电话:</span> {{ order.contactTel || '-' }}</p>
              <p><span>地址:</span> {{ order.address || '-' }}</p>
            </template>
            <p v-else class="pickup-label"><span>履约:</span> 到店自取</p>
          </div>

          <div class="order-items">
            <p class="items-title">商品明细:</p>
            <div v-for="(item, index) in order.foodList" :key="index" class="item-row">
              <span class="item-name">{{ item.foodName || '未知商品' }}&nbsp; &#165;{{ item.foodPrice }} &nbsp; × {{ item.quantity || 0 }}</span>
              <span class="item-price">¥ {{ (Number(item.foodPrice || 0) * Number(item.quantity || 0)).toFixed(2) }}</span>
            </div>
          </div>

          <div class="price-row">
            <template v-if="order.serviceMode !== 'PICKUP'">
              <br><span class="items-title">配送费: &#165;{{ Number(order.deliveryPrice || 0).toFixed(2) }}</span>
            </template>
            <template v-else>
              <br><span class="items-title pickup-label">无需配送费 · 到店自取</span>
            </template>
          </div>
          
          <div class="order-footer">
            <span class="order-time">下单时间: {{ formatTime(order.orderDate) }}</span>
            <span class="order-total">总计: ¥ {{ Number(order.orderTotal || 0).toFixed(2) }}</span>
          </div>
        </div>

        <div class="actions">
          <!-- 待接单：拒单 + 接单 -->
          <template v-if="order.orderState === 1">
            <button class="cancel-btn" @click.stop="rejectOrder(order.id)">拒单</button>
            <button class="confirm-btn" @click.stop="acceptOrder(order.id)">接单</button>
          </template>
          <template v-else-if="order.orderState === 2">
            <button class="confirm-btn" @click.stop="readyOrder(order.id)">确认出餐</button>
          </template>

          <div v-else class="fulfillment-tip"><i class="fas fa-route"></i> {{ fulfillmentTip(order.orderState, order) }}</div>
        </div>
      </li>
    </ul>


    <!-- 接单确认弹窗 -->
    <div v-if="showAcceptModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>确认操作</h3>
          <span class="close-btn" @click="closeModal">&times;</span>
        </div>
        <div class="modal-body">
          <p>确定要接单吗？</p>
        </div>
        <div class="modal-footer">
          <button class="modal-btn confirm-btn" @click="confirmAccept">确认</button>
          <button class="modal-btn cancel-btn" @click="closeModal">取消</button>
        </div>
      </div>
    </div>

    <!-- 拒单确认弹窗 -->
    <div v-if="showRejectModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>确认操作</h3>
          <span class="close-btn" @click="closeModal">&times;</span>
        </div>
        <div class="modal-body">
          <p>确定要拒单吗？</p>
        </div>
        <div class="modal-footer">
          <button class="modal-btn confirm-btn" @click="confirmReject">确认</button>
          <button class="modal-btn cancel-btn" @click="closeModal">取消</button>
        </div>
      </div>
    </div>

    <!-- 完成订单确认弹窗 -->
    <div v-if="showCompleteModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>确认操作</h3>
          <span class="close-btn" @click="closeModal">&times;</span>
        </div>
        <div class="modal-body">
          <p>确定要完成订单吗？</p>
        </div>
        <div class="modal-footer">
          <button class="modal-btn confirm-btn" @click="confirmComplete">确认</button>
          <button class="modal-btn cancel-btn" @click="closeModal">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed,onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import request from '../utils/request';
import { toast } from '../utils/toast';
import { getWebSocketUrl } from '../utils/endpoints';
import { orderStatusText } from '../utils/orderPresentation';
import { formatDateTime } from '../utils/formatters';
import { getStoredUser } from '../utils/auth';

export default {
  name: 'BusinessOrderManage',
  setup() {
    const router = useRouter();
    const route = useRoute();
    const loading = ref(false);
    const orders = ref([]);
    const businessId = ref(1);
    
    // 新增：商铺列表和选中状态
    const merchantList = ref([]);
    const selectedMerchantId = ref(null);
    const loadingMerchants = ref(false);

    // 订单状态标签
    const statusTabs = ref(['全部', '待接单', '待骑手', '配送中', '已完成', '已取消']);
    const activeStatusTab = ref(0);

    // 弹窗相关状态
    const showAcceptModal = ref(false);
    const showRejectModal = ref(false);
    const showCompleteModal = ref(false);
    const selectId = ref(0);

    // --- WebSocket 相关 ---
    const socket = ref(null);
    const userId = ref(null); // 存储当前商家用户ID
    let socketStopped = false;

    // 获取商铺列表
    const fetchMerchantList = async () => {
      loadingMerchants.value = true;
      try {
        const response = await request.get("/api/businesses/id_list");
        if (response.success) {
          merchantList.value = response.data || [];
          console.log("获取商铺列表成功:", merchantList.value);
          
          // 默认选择第一个商铺
          if (merchantList.value.length > 0) {
            selectedMerchantId.value = merchantList.value[0].merchantId;
            businessId.value = selectedMerchantId.value;
          }
        } else {
          console.error('获取商铺列表失败:', response.message);
        }
      } catch (error) {
        console.error("请求商铺列表失败:", error);
      } finally {
        loadingMerchants.value = false;
      }
    };

    // 选择商铺
    const selectMerchant = (merchantId) => {
      selectedMerchantId.value = merchantId;
      businessId.value = merchantId;
      fetchOrders(); // 重新加载该商铺的订单
    };

    // 获取订单列表
    const fetchOrders = async () => {
      if (!selectedMerchantId.value) return;
      
      loading.value = true;
      try {
        const response = await request.get("/api/orders/list/business", {
          params: { businessId: businessId.value }
        });

        if (response.success) {
          orders.value = response.data || [];
        } else {
          toast.error("获取订单列表失败");
        }
      } catch (error) {
        toast.error("获取订单列表失败");
      } finally {
        loading.value = false;
      }
    };

    // 切换状态标签
    const changeStatusTab = (index) => {
      activeStatusTab.value = index;
    };

    // 计算各状态订单数量
    const orderCounts = computed(() => {
      const counts = [0, 0, 0, 0, 0, 0];

      orders.value.forEach(order => {
        counts[0]++; // 全部
        if (order.orderState === 1) counts[1]++;
        else if ([2, 3].includes(order.orderState)) counts[2]++;
        else if ([4, 5, 6, 9].includes(order.orderState)) counts[3]++;
        else if (order.orderState === 7) counts[4]++;
        else if (order.orderState === 8) counts[5]++;
      });

      return counts;
    });

    // 过滤订单
    const filteredOrders = computed(() => {
      if (activeStatusTab.value === 0) return orders.value; // 全部

      const groups = {
        1: [1],
        2: [2, 3],
        3: [4, 5, 6, 9],
        4: [7],
        5: [8]
      };
      return orders.value.filter(order => groups[activeStatusTab.value].includes(order.orderState));
    });

    // 获取状态文本
    const getStatusText = (state, order = {}) => {
      return orderStatusText(state, order, 'merchant');
    };

    // 获取状态样式类
    const getStatusClass = (state) => {
      const classMap = {
        0: "unpaid",
        1: "pending",
        2: "accepted", 3: "accepted", 4: "accepted", 5: "accepted", 6: "accepted",
        7: "done", 8: "canceled", 9: "unpaid"
      };
      return classMap[state] || "";
    };

    // 格式化时间
    const formatTime = (timeString) => {
      return formatDateTime(timeString);
    };

    // 接单
    const acceptOrder = (id) => {
      selectId.value = id;
      showAcceptModal.value = true;
    };

    // 确认接单
    const confirmAccept = async () => {
      if (selectId.value === 0) return;

      try {
        const response = await request.post(`/api/v1/orders/${selectId.value}/merchant-accept`);
        if (response.success) {
          toast.success("接单成功");
          fetchOrders(); // 重新加载订单
        } else {
          toast.error("接单失败: " + response.message);
        }
      } catch (error) {
        toast.error("接单失败，请稍后重试");
      } finally {
        closeModal();
      }
    };

    const readyOrder = async (id) => {
      try {
        const response = await request.post(`/api/v1/orders/${id}/merchant-ready`);
        if (response.success) { toast.success(orderById(id)?.serviceMode === 'PICKUP' ? '已确认出餐，等待顾客到店取餐' : '已确认出餐，配送任务已发布'); fetchOrders(); }
        else toast.error(response.message || '确认出餐失败');
      } catch (error) { toast.error(error.response?.data?.message || '确认出餐失败，请稍后重试'); }
    };

    // 拒单
    const rejectOrder = (id) => {
      selectId.value = id;
      showRejectModal.value = true;
    };

    // 确认拒单
    const confirmReject = async () => {
      if (selectId.value === 0) return;

      try {
        const response = await request.post(`/api/v1/orders/${selectId.value}/merchant-reject`);
        if (response.success) {
          toast.success("拒绝成功");
          fetchOrders(); // 重新加载订单
        } else {
          toast.error(response.message);
        }
      } catch (error) {
        toast.error("拒绝失败，请重试");
      } finally {
        closeModal();
      }
    };

    // 完成订单
    const completeOrder = (id) => {
      selectId.value = id;
      showCompleteModal.value = true;
    };

    // 确认完成订单
    const confirmComplete = async () => {
      if (selectId.value === 0) return;

      try {
        const response = await request.put("/api/orders/status?orderState=3&orderId=" + selectId.value);
        if (response.success) {
          toast.success("确认成功");
          fetchOrders();
        } else {
          toast.error(response.message);
        }
      } catch (error) {
        toast.error("确认失败,请稍后重试");
      } finally {
        closeModal();
      }
    };

    const orderById = id => orders.value.find(item => item.id === id);
    const fulfillmentTip = (state, order = {}) => {
      if (state === 4 && order.serviceMode === 'PICKUP') return '等待顾客到店取餐';
      return ({ 2: '餐品制作中，完成后请确认出餐', 3: '任务已进入骑手大厅', 4: '骑手正在前往商家', 5: '骑手配送中', 6: '等待顾客确认', 7: '订单已闭环', 8: '订单已取消', 9: '调度员正在处理' }[state] || '履约中');
    };

    // 关闭弹窗
    const closeModal = () => {
      showAcceptModal.value = false;
      showRejectModal.value = false;
      showCompleteModal.value = false;
      selectId.value = 0;
    };

    // 预留订单详情入口，当前卡片操作不调用。
    const goDetail = (order) => {
      router.push({
        path: '/orderDetail',
        query: { orderId: order.id }
      });
    };

    // 初始化WebSocket
    const initWebSocket = () => {
      if (socketStopped) return;
      const user = getStoredUser();
      userId.value = user?.id || null;
      if (!userId.value) return;

      socket.value = new WebSocket(getWebSocketUrl(`/ws/${userId.value}`));

      socket.value.onopen = () => {
        console.log("WebSocket 连接成功");
      };

      socket.value.onmessage = (event) => {
        const message = JSON.parse(event.data);
        console.log("收到WebSocket消息:", message);
        // 收到消息后，刷新当前商铺的订单列表
        fetchOrders();
      };

      socket.value.onclose = () => {
        console.log("WebSocket 连接关闭");
        // 断线重连（可选）
        if (!socketStopped) setTimeout(initWebSocket, 2000);
      };

      socket.value.onerror = (err) => {
        console.error("WebSocket 错误:", err);
      };
    };

    // 关闭WebSocket（组件销毁时调用）
    const closeWebSocket = () => {
      socketStopped = true;
      if (socket.value && socket.value.readyState === WebSocket.OPEN) {
        socket.value.close();
      }
    };

    onMounted(() => {
      socketStopped = false;
      initWebSocket();
      // 先获取商铺列表，然后自动加载第一个商铺的订单
      fetchMerchantList().then(() => {
        if (selectedMerchantId.value) {
          fetchOrders();
        }
      });
    });
    onUnmounted(() => {
      // 组件销毁时关闭WebSocket
      closeWebSocket();
    });

    return {
      loading,
      orders,
      merchantList,
      selectedMerchantId,
      statusTabs,
      activeStatusTab,
      orderCounts,
      filteredOrders,
      fetchOrders,
      selectMerchant,
      changeStatusTab,
      getStatusText,
      getStatusClass,
      formatTime,
      acceptOrder,
      rejectOrder,
      completeOrder,
      showAcceptModal,
      showRejectModal,
      showCompleteModal,
      closeModal,
      confirmAccept,
      confirmReject,
      confirmComplete,
      fulfillmentTip,
      readyOrder,
      goDetail
    };
  }
};
</script>

<style scoped>
.wrapper {
  width: 100%;
  min-height: 100vh;
  background: #f5f7fa;
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

/* 商铺选择栏 */
.merchant-selector {
  background:#f0f5f9;
  box-shadow: 0 2vw 4vw rgba(102, 126, 234, 0.2);
  padding: 3vw 0 0 0;
  position: fixed;
  top: 100px;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 600px;
  z-index: 999;
  height: 17vw;
}

.merchant-header {
  padding: 0 4vw 2vw 4vw;
}

.merchant-title {
  font-size: 3.8vw;
  color: #fff;
  font-weight: 600;
  opacity: 0.9;
}

.merchant-tabs {
  display: flex;
  align-items: center;
  padding: 0 16px;
  overflow-x: auto;
  white-space: nowrap;
  padding-bottom: 12px;
  scrollbar-width: none;
  -ms-overflow-style: none;
  touch-action: pan-x;
  height: 76px;
  scroll-behavior: auto; /* 禁用平滑滚动，更直接 */
  -webkit-overflow-scrolling: touch; /* 启用动量滚动 */
}

.merchant-tabs::-webkit-scrollbar {
  display: none;
}

.merchant-no-business {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  font-size: 4vw;
  color: #a7a6a6;
  font-weight: 500;
  text-align: center;
}

.merchant-tabs li {
  margin-right: 16px;
  padding: 12px 16px;
  font-size: 16px;
  color: #2f3335;
  background: #d0dff1;
  border-radius: 8px;
  cursor: pointer;
  flex: 0 0 auto;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  touch-action: pan-x;
}

.merchant-tabs li.active {
  background: #0097ff;
  color: #ffffff;
  font-weight: 600;
  transform: translateY(-0.5vw);
  box-shadow: 0 1vw 2vw rgba(0, 0, 0, 0.2);
}

.merchant-tabs li:not(.active):hover {
  background: #f7fbfc;
  transform: translateY(-0.2vw);
}

/* 订单状态分类 - 修复滑动问题 */
.tabs {
  display: flex;
  align-items: center;
  padding: 0 16px;
  background: #c3ecfe;
  border-bottom: 1px solid #f0f0f0;
  overflow-x: auto;
  white-space: nowrap;
  position: fixed;
  top: 176px; /* 顶部标题栏100px + 商铺选择栏76px */
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 600px;
  z-index: 998;
  height: 64px;
  scrollbar-width: none;
  -ms-overflow-style: none;
  touch-action: pan-x;
  box-shadow: 0 1vw 2vw rgba(0, 0, 0, 0.1);
  border-radius: 0 0 12px 12px;
  scroll-behavior: auto; /* 禁用平滑滚动，更直接 */
  -webkit-overflow-scrolling: touch; /* 启用动量滚动 */
}

.tabs::-webkit-scrollbar {
  display: none;
}

.tabs li {
  margin-right: 24px;
  padding: 12px 0;
  font-size: 16px;
  color: #666;
  position: relative;
  cursor: pointer;
  flex: 0 0 auto;
  touch-action: pan-x;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tabs li.active {
  color: #3490de;
  font-weight: 600;
}

.tabs li.active::after {
  content: "";
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  height: 3px;
  background: #409eff;
  border-radius: 2px;
}

/* 调整订单列表的上边距，为固定栏留出空间 */
.order-list {
  padding: 16px;
  margin-top: 264px; /* 固定标题、店铺和状态栏后留出间距 */
  margin-bottom: 90px;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

/* 调整空状态和加载状态的上边距 */
.loading, .empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 48px;
  font-size: 16px;
  color: #999;
  background: white;
  margin: 24px auto;
  border-radius: 10px;
  margin-top: 288px;
  max-width: 600px;
}

/* 其他样式保持不变 */
.empty-state {
  flex-direction: column;
}

.empty-state img {
  width: 140px;
  height: 140px;
  margin-bottom: 20px;
  opacity: 0.5;
}

.order-item {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(0,0,0,.05);
  padding: 20px;
  margin-bottom: 20px;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 14px;
  border-bottom: 1px solid #f5f5f5;
  margin-bottom: 14px;
}

.order-id {
  font-size: 16px;
  color: #999;
}

.status-badge {
  padding: 5px 10px;
  border-radius: 5px;
  font-size: 14px;
  font-weight: 500;
  position: relative;
  z-index: 10; /* 确保在最上层 */
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
  margin-bottom: 18px;
}

.customer-info p {
  font-size: 16px;
  color: #333;
  margin: 8px 0;
  display: flex;
}

.customer-info span {
  color: #666;
  margin-right: 10px;
  min-width: 56px;
}

.items-title {
  font-size: 16px;
  color: #333;
  font-weight: 500;
  margin: 16px 0 10px 0;
  border-top: 1px solid #f0f0f0;
  padding-top: 14px;
}

.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  font-size: 15px;
  color: #333;
}

.item-name {
  flex: 1;
}

.item-quantity {
  margin: 0 12px;
  color: #666;
}

.item-price {
  font-weight: 500;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #f0f0f0;
}

.order-time {
  font-size: 13px;
  color: #999;
}

.order-total {
  font-size: 18px;
  color: #ff6b00;
  font-weight: bold;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 14px;
  border-top: 1px solid #f5f5f5;
}

.actions button {
  padding: 10px 18px;
  border-radius: 8px;
  font-size: 15px;
  cursor: pointer;
  border: none;
}

.cancel-btn {
  background: #fff;
  color: #ff4d4f;
}

.confirm-btn {
  background: #409eff;
  color: #fff;
}

.complete-btn {
  background: #52c41a;
  color: #fff;
}

.detail-btn {
  background: #f5f5f5;
  color: #666;
}

/* 弹窗样式 */
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

/* 移动端响应式样式 */
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
  
  .merchant-selector {
    top: 90px;
    max-width: 100vw;
    transform: none;
    left: 0;
  }
  
  .tabs {
    top: calc(90px + 17vw);
    max-width: 100vw;
    transform: none;
    left: 0;
  }
  
  .order-list {
    margin-top: calc(90px + 17vw + 14vw + 4vw);
    max-width: 100vw;
    width: 100vw;
  }
  
  .loading, .empty-state {
    margin-top: calc(90px + 17vw + 14vw + 7vw);
    max-width: 100vw;
  }
}

/* 与用户端统一的朴素蓝白头部和筛选栏 */
.wrapper { background: #f5f9fd; color: #24405c; }
.top-background { height: 64px; background: #0097ff; background-image: none; border-radius: 0; box-shadow: 0 1px 0 rgba(0,83,145,.15); }
.top-background::before { display: none; }
.top-background h1 { font-size: 20px; letter-spacing: 0; text-shadow: none; }
.merchant-selector { top: 64px; height: 56px; padding: 6px 0 0; background: #fff; box-shadow: none; border-bottom: 1px solid #dcebf7; }
.merchant-header { padding: 0 16px 4px; }
.merchant-tabs { height: 42px; padding: 0 16px 5px; }
.merchant-tabs li { margin-right: 8px; padding: 6px 12px; font-size: 13px; border-radius: 6px; background: #edf6fc; border: 1px solid #dcebf7; backdrop-filter: none; }
.merchant-tabs li.active { background: #e5f3ff; color: #0879c7; border-color: #a9d6f4; box-shadow: none; transform: none; }
.tabs { top: 120px; height: 46px; background: #fff; box-shadow: none; border-radius: 0; border-bottom: 1px solid #dcebf7; }
.tabs li { margin-right: 22px; padding: 12px 0; font-size: 13px; }
.tabs li.active { color: #0879c7; }
.order-list { margin-top: 184px; padding: 12px 16px 84px; }
.order-item { border: 1px solid #e1edf7; border-radius: 10px; box-shadow: 0 2px 8px rgba(36,91,132,.06); }
.order-item .order-content, .order-item .order-footer { min-width: 0; }
.order-item p, .order-item span { overflow-wrap: anywhere; }
@media (max-width: 480px) {
  .top-background { height: 64px; }
  .merchant-selector { top: 64px; }
  .tabs { top: 120px; left: 0; transform: none; max-width: 100vw; }
  .order-list { margin-top: 184px; width: 100vw; max-width: 100vw; padding: 12px 12px 84px; }
}
</style>
