<template>
  <!-- 管理端商家管理 -->
  <div class="wrapper">
    <!-- 顶部蓝色栏，与 OrderList 保持一致 -->
    <header class="topbar">
      <p>订单</p>
    </header>

    <!-- 页面标题，与 OrderList 保持一致 -->
    <div class="page-title">订单管理中心</div>

    <!-- 店铺切换，沿用 OrderList 的 tabs 样式（支持左右滑动） -->
    <ul class="tabs tabs-scroll">
      <li
        v-for="(s, idx) in shops"
        :key="s.businessId || idx"
        :class="{ active: activeShopIndex === idx }"
        @click="selectShop(idx)"
      >
        {{ s.businessName || `店铺${idx + 1}` }}
      </li>
    </ul>

    <!-- 订单状态分类 -->
    <ul class="tabs">
      <li
        v-for="(t, i) in statusTabs"
        :key="t"
        :class="{ active: activeStatusTab === i }"
        @click="activeStatusTab = i"
      >
        {{ t }}
      </li>
    </ul>

    <!-- 订单列表，沿用 OrderList 的结构与样式 -->
    <!-- 商家端：点击订单项跳转到订单详情（共用 ListDetail） -->
    <ul class="order-list">
      <li v-for="o in currentOrdersFiltered" :key="o.orderId" class="order-item" @click="goDetail(o)" title="查看订单详情">
        <img class="thumb" src="/baozi.jpg" alt="thumb">
        <div class="meta">
          <p class="name">订单号：#{{ o.orderId }}</p>
          <p class="sub">顾客：{{ o.userName || '-' }}</p>
          
          <p class="price">¥ {{ Number(o.orderTotal).toFixed(2) }}</p>
          <span class="status-badge" :class="badgeClass(o.status)">{{ statusText(o.status) }}</span>
        </div>
        <div class="actions">
          <template v-if="o.status === '待接单'">
            <button class="cancel-btn" @click.stop="reject(o)">拒单</button>
            <button class="confirm-btn" @click.stop="accept(o)">接单</button>
          </template>
          <template v-else-if="o.status === '已接单'">
            <button class="confirm-btn" @click.stop="finish(o)">完成</button>
          </template>
          <template v-else-if="o.status === '已取消'">
            <button class="cancel-btn disabled" disabled @click.stop>已取消</button>
          </template>
          <template v-else>
            <button class="confirm-btn disabled" disabled @click.stop>已完成</button>
          </template>
        </div>
      </li>
    </ul>

    <BusinessFooter />
  </div>
  </template>

<script>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';
import BusinessFooter from '@/components/BusinessFooter.vue';
import { toast } from '@/utils/toast';

export default {
  name: 'BusinessOrderManage',
  components: { BusinessFooter },
  setup() {
    const route = useRoute();
    const router = useRouter();

    const shops = ref([]);
    const activeShopIndex = ref(0);
    const orderMap = ref({}); // businessId -> orders
    const statusTabs = ref(['全部', '待接单', '已接单', '已取消', '已完成']);
    const activeStatusTab = ref(0);

    const currentBusinessId = computed(() => shops.value[activeShopIndex.value]?.businessId);
    const currentOrders = computed(() => orderMap.value[currentBusinessId.value] || []);
    const currentOrdersFiltered = computed(() => {
      const all = currentOrders.value;
      switch (activeStatusTab.value) {
        case 1: return all.filter(o => o.status === '待接单');
        case 2: return all.filter(o => o.status === '已接单');
        case 3: return all.filter(o => o.status === '已取消');
        case 4: return all.filter(o => o.status === '已完成');
        default: return all;
      }
    });

    const tabWithCount = (t, i) => {
      const counts = {
        all: currentOrders.value.length,
        pending: currentOrders.value.filter(o => o.status === '待接单').length,
        accepted: currentOrders.value.filter(o => o.status === '已接单').length,
        canceled: currentOrders.value.filter(o => o.status === '已取消').length,
        done: currentOrders.value.filter(o => o.status === '已完成').length
      };
      const map = [counts.all, counts.pending, counts.accepted, counts.canceled, counts.done];
      return `${t} (${map[i]})`;
    };

    const statusText = (s) => s;
    const badgeClass = (s) => {
      switch (s) {
        case '待接单': return 'pending';
        case '已接单': return 'accepted';
        case '已取消': return 'canceled';
        case '已完成': return 'done';
        default: return '';
      }
    };

    const ensureBusinessLogin = () => {
      const bu = sessionStorage.getItem('businessUser') ? JSON.parse(sessionStorage.getItem('businessUser')) : null;
      if (!bu || !bu.isBusiness) {
        toast.error("请先登录商家账号！");
        router.push('/businessLogin');
        return null;
      }
      return bu;
    };

    const normalizeOrder = (raw) => {
      return {
        orderId: raw.orderId,
        orderTotal: raw.orderTotal || 0,
        userName: raw.userName || raw.user?.userName,
        itemsText: raw.itemsText || '—',
        status: raw.status // 字符串：待接单/已接单/已取消/已完成
      };
    };

    const loadShopsAndOrders = async (businessId) => {
      // 店铺切换条：当前业务只展示该商家的多个门店
      try {
        const bizResp = await axios.post('BusinessController/listStoresByOwner', { businessId });
        const list = Array.isArray(bizResp.data) && bizResp.data.length > 0 ? bizResp.data : null;
        if (list) {
          shops.value = list;
        } else {
          // ===== 虚拟店铺 开始 =====
          shops.value = [
            { businessId: `${businessId}-A`, businessName: '美味小厨' },
            { businessId: `${businessId}-B`, businessName: '街角奶茶店' },
            { businessId: `${businessId}-C`, businessName: '深夜食堂' }
          ];
          // ===== 虚拟店铺 结束 =====
        }
      } catch (e) {
        // ===== 虚拟店铺 开始 =====
        shops.value = [
          { businessId: `${businessId}-A`, businessName: '美味小厨', businessAddress: '和平区南京路188号', deliveryPrice: 5 },
          { businessId: `${businessId}-B`, businessName: '街角奶茶店', businessAddress: '南开区鼓楼北街20号', deliveryPrice: 3 },
          { businessId: `${businessId}-C`, businessName: '深夜食堂', businessAddress: '河西区围堤道88号', deliveryPrice: 4 }
        ];
        // ===== 虚拟店铺 结束 =====
      }

      // 为每个店铺加载订单
      for (let i = 0; i < shops.value.length; i++) {
        const sid = shops.value[i].businessId;
        try {
          const resp = await axios.post('OrdersController/listOrdersByBusinessId', { businessId: sid });
          const arr = Array.isArray(resp.data) ? resp.data.map(item => normalizeOrder(mapBackendToMerchantStatus(item))) : [];
          orderMap.value[sid] = seedMock(arr, i);
        } catch (e) {
          orderMap.value[sid] = seedMock([], i);
        }
      }
    };

    const mapBackendToMerchantStatus = (o) => {
      // 后端若使用数字状态，可在此转换
      // 0:未支付 1:已接单 ... 这里只映射为商家视角的四态
      let status = '待接单';
      if (o.orderState === -1) status = '已取消';
      else if (o.orderState === 1) status = '已接单';
      else if (o.orderState === 2) status = '已完成';
      return { ...o, status };
    };

    // 演示/联调用：补充一些订单
    const seedMock = (base, shopIndex = 0) => {
      if (base.length > 0) return base;
      // ===== 虚拟订单 开始 =====
      const baseId = 20231027000 + shopIndex * 100;
      const nowId = (n) => baseId + n;
      const presets = [
        [
          { orderId: nowId(1), userName: '张三', itemsText: '红烧肉套餐 x1，可乐 x1', orderTotal: 35, status: '待接单' },
          { orderId: nowId(2), userName: '李四', itemsText: '宫保鸡丁 x1，米饭 x2', orderTotal: 28, status: '已接单' },
          { orderId: nowId(3), userName: '王五', itemsText: '麻辣香锅 x1', orderTotal: 56, status: '已完成' },
          { orderId: nowId(4), userName: '赵六', itemsText: '鱼香肉丝 x1，米饭 x1', orderTotal: 26, status: '已取消' }
        ],
        [
          { orderId: nowId(1), userName: '小李', itemsText: '黑糖珍珠奶茶 x2', orderTotal: 24, status: '待接单' },
          { orderId: nowId(2), userName: '小王', itemsText: '芝士奶盖绿茶 x1', orderTotal: 16, status: '已接单' },
          { orderId: nowId(3), userName: '小赵', itemsText: '抹茶拿铁 x1', orderTotal: 18, status: '已完成' }
        ],
        [
          { orderId: nowId(1), userName: '夜猫', itemsText: '牛肉拉面 x1', orderTotal: 22, status: '待接单' },
          { orderId: nowId(2), userName: '加班狗', itemsText: '炒饭 x1，可乐 x1', orderTotal: 20, status: '已接单' },
          { orderId: nowId(3), userName: '码农', itemsText: '煎饺 x1', orderTotal: 15, status: '已完成' }
        ]
      ];
      const pick = presets[shopIndex % presets.length];
      return pick;
      // ===== 虚拟订单 结束 =====
    };

    // 操作
    const accept = (o) => { o.status = '已接单'; };
    const reject = (o) => { o.status = '已取消'; };
    const cancel = (o) => { o.status = '已取消'; };
    const finish = (o) => { o.status = '已完成'; };

    // 解析 itemsText -> 明细数组的简易兜底
    const parseItemsText = (text = '', total = 0) => {
      if (!text) return [];
      const parts = text.split(/[，,]/).map(s => s.trim()).filter(Boolean);
      const items = parts.map(p => {
        const m = p.match(/(.+?)\s*[x×]\s*(\d+)/i);
        return {
          foodName: m ? m[1].trim() : p,
          quantity: m ? Number(m[2]) : 1,
          foodPrice: 0
        };
      });
      const qtySum = items.reduce((s, it) => s + (Number(it.quantity) || 0), 0) || 1;
      const unit = Number(total) / qtySum;
      items.forEach(it => { it.foodPrice = unit; });
      return items;
    };

    const goDetail = (o) => {
      const biz = shops.value[activeShopIndex.value] || {};
      // 映射为 ListDetail 可识别的结构
      const mappedOrder = {
        orderId: o.orderId,
        orderTotal: o.orderTotal,
        // 提供字符串状态，ListDetail 将兼容渲染
        status: o.status,
        // 尽量给出数值状态（用于后端一致性），无法精准映射时留空
        orderState: o.status === '已取消' ? -1 : (o.status === '已完成' ? 2 : (o.status === '已接单' ? 1 : 0)),
        userName: o.userName || '-',
        userPhone: o.userPhone || '—',
        userAddress: o.userAddress || '—',
        business: {
          businessName: biz.businessName || '—',
          businessAddress: biz.businessAddress || '地址未知',
          deliveryPrice: biz.deliveryPrice ?? 0
        },
        // 优先后端明细；如没有，解析 itemsText 兜底
        detailet: Array.isArray(o.detailet) && o.detailet.length > 0 ? o.detailet : parseItemsText(o.itemsText, o.orderTotal)
      };
      try { sessionStorage.setItem('selectedOrder', JSON.stringify(mappedOrder)); } catch (e) {}
      router.push({ path: '/listDetail', query: { orderId: o.orderId } });
    };

    const selectShop = (idx) => {
      activeShopIndex.value = idx;
      activeStatusTab.value = 0; // 重置为“全部”
    };

    onMounted(() => {
      const bu = ensureBusinessLogin();
      if (!bu) return;
      loadShopsAndOrders(bu.businessId);
    });

    return {
      shops,
      activeShopIndex,
      currentOrders,
      currentOrdersFiltered,
      tabWithCount,
      accept,
      reject,
      cancel,
      finish,
      statusText,
      badgeClass,
      goDetail,
      selectShop,
      statusTabs,
      activeStatusTab
    };
  }
};
</script>

<style scoped>
/* 直接复用 OrderList 的样式命名，确保风格一致 */
.wrapper {
  width: 100%;
  height: 100%;
  background: #fff;
}
.topbar {
  width: 100%;
  height: 12vw;
  background-color: #409eff;
  color: #fff;
  font-size: 4.8vw;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: center;
}
.page-title {
  margin-top: 12vw;
  padding: 4vw;
  font-size: 4vw;
  color: #333;
}
.tabs {
  display: flex;
  align-items: center;
  padding: 0 4vw;
  border-bottom: 1px solid #f0f0f0;
}
.tabs-scroll {
  overflow-x: auto;
  white-space: nowrap;
  scrollbar-width: none; /* Firefox */
}
.tabs-scroll::-webkit-scrollbar { /* Chrome/Safari */
  display: none;
}
.tabs li {
  margin-right: 6vw;
  padding: 2vw 0;
  font-size: 3.8vw;
  color: #666;
  position: relative;
  cursor: pointer;
  flex: 0 0 auto;
}
.tabs li.active {
  color: #333;
  font-weight: 600;
}
.tabs li.active::after {
  content: "";
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  height: .8vw;
  background: #409eff;
  border-radius: .4vw;
}
.order-list {
  padding: 4vw;
}
.order-item {
  display: flex;
  align-items: center;
  margin-bottom: 4vw;
  background: #fff;
  border-radius: 1.6vw;
  box-shadow: 0 2px 10px rgba(0,0,0,.05);
  padding: 3vw;
}
.thumb {
  width: 28vw;
  height: 28vw;
  object-fit: cover;
  border-radius: 1.2vw;
  margin-right: 3vw;
}
.meta { flex: 1; }
.name { font-size: 4vw; color: #333; }
.sub { font-size: 3.2vw; color: #999; margin-top: 1vw; }
.price { font-size: 4.2vw; color: #3a78ff; margin-top: 2vw; }
.status-badge {
  display: inline-block;
  margin-top: 1.2vw;
  padding: .6vw 1.6vw;
  border-radius: 1.2vw;
  font-size: 3vw;
  line-height: 1;
  background: #e6eefb;
  color: #8aa8e5;
}
.status-badge.done { background: #eef0f3; color: #97a0af; }
.status-badge.pending { background: #e6f4ff; color: #1e80ff; }
.status-badge.canceled { background: #fdeeee; color: #e15656; }
.status-badge.accepted { background: #e8f5e9; color: #43a047; }
.actions { display: flex; flex-direction: column; align-items: center; }
.confirm-btn {
  background: #1e80ff;
  color: #fff;
  border: none;
  border-radius: 1.6vw;
  padding: 2.4vw 5vw;
  font-size: 4.6vw;
  cursor: pointer;
}
.confirm-btn.disabled { background: #e6e9ef; color: #fff; cursor: not-allowed; }
.cancel-btn {
  background: #ffffff;
  color: #e15656;
  border: 1px solid #f3caca;
  border-radius: 1.6vw;
  padding: 2vw 5vw;
  font-size: 4.2vw;
  margin-top: 1.6vw;
  cursor: pointer;
}
.cancel-btn.disabled { background: #f5f5f5; color: #bbb; border-color: #eee; cursor: not-allowed; }
</style>


