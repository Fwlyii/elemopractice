<template>
  <main class="rider-page">
    <header class="topbar">
      <div class="brand"><span class="logo"><i class="fas fa-motorcycle"></i></span><div><b>配送工作台</b><small>骑手服务中心</small></div></div>
      <div class="top-actions">
        <button class="user-side" @click="$router.push('/index')"><i class="fas fa-home"></i> 顾客端</button>
        <button class="avatar" @click="$router.push('/myInformation?role=rider')">{{ profile?.realName?.slice(0,1) || '骑' }}</button>
      </div>
    </header>

    <section v-if="loading" class="loading-card"><span class="spinner"></span>正在同步配送网络…</section>
    <section v-else-if="!profile || profile.auditStatus !== 1" class="loading-card">
      <i class="fas fa-id-card"></i><h2>骑手资质尚未生效</h2><p>请先完成申请与审核。</p>
      <button class="primary" @click="$router.push('/rider/apply')">查看申请状态</button>
    </section>

    <template v-else>
      <section class="hero-card">
        <div>
          <p class="date">{{ todayText }}</p>
          <h1>{{ greeting }}，{{ profile.realName }}</h1>
          <p class="hero-note">{{ profile.online ? '已上线，可以查看和接取附近订单。' : '当前为休息状态，上线后可接取新任务。' }}</p>
        </div>
        <button class="online-switch" :class="{ online: profile.online }" :disabled="switching" @click="toggleOnline">
          <span class="switch-dot"></span><span><b>{{ profile.online ? '接单中' : '已休息' }}</b><small>{{ profile.online ? '点击下线' : '点击上线' }}</small></span>
        </button>
      </section>

      <section class="stat-grid">
        <article><span class="stat-icon green"><i class="fas fa-route"></i></span><div><small>累计里程</small><strong>{{ number(profile.totalDistance, 1) }}<em> km</em></strong></div></article>
        <article><span class="stat-icon blue"><i class="fas fa-check-circle"></i></span><div><small>完成配送</small><strong>{{ profile.completedOrders || 0 }}<em> 单</em></strong></div></article>
        <article><span class="stat-icon orange"><i class="fas fa-wallet"></i></span><div><small>累计收入</small><strong><em>¥</em>{{ number(profile.totalIncome, 2) }}</strong></div></article>
        <article><span class="stat-icon purple"><i class="fas fa-layer-group"></i></span><div><small>当前任务</small><strong>{{ activeTasks.length }}<em> 单</em></strong></div></article>
      </section>

      <section class="workspace">
        <nav class="tabs">
          <button v-for="tab in visibleTabs" :key="tab.key" :class="{ active: activeTab === tab.key }" @click="selectTab(tab.key)">
            <i :class="tab.icon"></i>{{ tab.label }}<b v-if="countFor(tab.key)">{{ countFor(tab.key) }}</b>
          </button>
          <button class="refresh" @click="refreshAll"><i class="fas fa-sync-alt" :class="{ spin: refreshing }"></i> 刷新</button>
        </nav>

        <div v-if="activeTab === 'available' && !profile.online" class="empty">
          <span><i class="fas fa-power-off"></i></span><h3>上线后接收附近订单</h3><p>休息状态不会为你分配新任务。</p><button class="primary small" @click="toggleOnline">立即上线</button>
        </div>
        <div v-else-if="visibleTasks.length === 0" class="empty">
          <span><i class="fas fa-mug-hot"></i></span><h3>{{ emptyTitle }}</h3><p>配送网络会自动同步最新状态。</p>
        </div>

        <div v-else class="task-grid">
          <article v-for="task in visibleTasks" :key="task.id" class="task-card" :class="task.taskStatus.toLowerCase()">
            <div class="task-head">
              <div><span class="task-id">DELIVERY #{{ task.id }}</span><h3>{{ task.businessName }}</h3></div>
              <span class="status" :class="task.taskStatus.toLowerCase()">{{ taskStatus(task.taskStatus) }}</span>
            </div>
            <div class="route">
              <div class="route-mark"><i class="fas fa-store"></i><span></span><i class="fas fa-map-marker-alt"></i></div>
              <div class="route-copy">
                <div><small>取餐地址</small><p>{{ task.businessAddress }}</p></div>
                <div><small>送达地址</small><p>{{ task.deliveryAddress }}</p><em v-if="task.contactName && task.contactName !== '接单后可见'">{{ task.contactName }} · {{ task.contactTel }}</em></div>
              </div>
            </div>
            <div class="task-meta">
              <span><i class="fas fa-road"></i><b>{{ number(task.distanceKm,1) }}</b> km</span>
              <span><i class="fas fa-coins"></i>预计 <b>¥{{ number(task.riderFee,2) }}</b></span>
              <span><i class="fas fa-receipt"></i>订单 ¥{{ number(task.orderTotal,2) }}</span>
            </div>
            <div class="task-actions">
              <button v-if="task.taskStatus === 'WAITING_RIDER'" class="accept" :disabled="actingId === task.id" @click="act(task, 'accept')">接取订单</button>
              <template v-else-if="task.taskStatus === 'ACCEPTED'">
                <button class="ghost" @click="navigate(task.businessAddress)"><i class="fas fa-location-arrow"></i> 导航去商家</button><button class="accept" @click="act(task, 'arrive-store')">我已到店</button>
              </template>
              <template v-else-if="task.taskStatus === 'ARRIVED_STORE'">
                <button class="ghost danger" @click="openException(task)">上报异常</button><button class="accept" @click="act(task, 'pickup')">确认取餐</button>
              </template>
              <template v-else-if="task.taskStatus === 'DELIVERING'">
                <button class="ghost" @click="navigate(task.deliveryAddress)"><i class="fas fa-location-arrow"></i> 导航去顾客</button><button class="accept" @click="act(task, 'deliver')">确认送达</button>
                <button class="exception-link" @click="openException(task)">遇到配送问题？</button>
              </template>
              <div v-else-if="task.taskStatus === 'DELIVERED'" class="waiting-confirm"><i class="fas fa-clock"></i> 已送达，等待顾客确认</div>
              <div v-else-if="task.taskStatus === 'EXCEPTION'" class="waiting-confirm warning"><i class="fas fa-headset"></i> 管理员正在处理异常</div>
              <div v-else class="complete-note"><i class="fas fa-check"></i> {{ task.completedTime ? '完成于 ' + formatTime(task.completedTime) : '任务已结束' }}</div>
            </div>
          </article>
        </div>
      </section>
    </template>

    <div v-if="exceptionModal" class="modal-mask" @click.self="exceptionModal = null">
      <form class="modal" @submit.prevent="submitException">
        <span class="modal-icon"><i class="fas fa-exclamation-triangle"></i></span><h2>上报配送异常</h2><p>任务 #{{ exceptionModal.id }} 将暂停履约，并立即通知调度管理员。</p>
        <label>异常类型<select v-model="exceptionForm.exceptionType"><option v-for="item in exceptionTypes" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
        <label>情况说明<textarea v-model.trim="exceptionForm.description" maxlength="500" placeholder="说明现场情况，便于调度快速处理" required></textarea></label>
        <div class="modal-actions"><button type="button" class="ghost" @click="exceptionModal = null">取消</button><button class="accept" type="submit">提交异常</button></div>
      </form>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request';
import { toast } from '@/utils/toast';
import { getWebSocketUrl } from '@/utils/endpoints';

const route = useRoute();
const router = useRouter();
const profile = ref(null), availableTasks = ref([]), activeTasks = ref([]), historyTasks = ref([]);
const loading = ref(true), refreshing = ref(false), switching = ref(false), actingId = ref(null), activeTab = ref('available');
const exceptionModal = ref(null);
const socket = ref(null);
let reconnectTimer = null;
let destroyed = false;
const exceptionForm = reactive({ exceptionType: 'STORE_DELAY', description: '' });
const tabs = [
  { key:'available', label:'接单广场', icon:'fas fa-compass' },
  { key:'active', label:'进行中', icon:'fas fa-route' },
  { key:'history', label:'历史配送', icon:'fas fa-history' }
];
const exceptionTypes = [
  {value:'STORE_DELAY',label:'商家出餐延迟'}, {value:'CUSTOMER_UNREACHABLE',label:'无法联系顾客'},
  {value:'ADDRESS_ERROR',label:'收货地址异常'}, {value:'VEHICLE_FAILURE',label:'车辆故障'}, {value:'OTHER',label:'其他问题'}
];

const todayText = new Intl.DateTimeFormat('zh-CN',{month:'long',day:'numeric',weekday:'long'}).format(new Date());
const greeting = computed(() => { const h = new Date().getHours(); return h < 11 ? '早上好' : h < 14 ? '中午好' : h < 18 ? '下午好' : '晚上好'; });
const visibleTasks = computed(() => activeTab.value === 'available' ? availableTasks.value : activeTab.value === 'active' ? activeTasks.value : historyTasks.value);
const emptyTitle = computed(() => activeTab.value === 'available' ? '暂时没有新任务' : activeTab.value === 'active' ? '没有进行中的配送' : '还没有历史配送');
const countFor = key => key === 'available' ? availableTasks.value.length : key === 'active' ? activeTasks.value.length : historyTasks.value.length;
const visibleTabs = computed(() => activeTab.value === 'available'
  ? [{ key: 'available', label: '附近订单', icon: 'fas fa-compass' }]
  : tabs.filter(tab => tab.key !== 'available'));
const number = (value, digits) => Number(value || 0).toFixed(digits);
const taskStatus = value => ({WAITING_RIDER:'待接单',ACCEPTED:'前往商家',ARRIVED_STORE:'已到店',DELIVERING:'配送中',DELIVERED:'已送达',COMPLETED:'已完成',EXCEPTION:'异常处理中',CANCELLED:'已取消'}[value] || value);
const formatTime = value => new Date(value).toLocaleString('zh-CN',{month:'numeric',day:'numeric',hour:'2-digit',minute:'2-digit'});
const selectTab = tab => {
  activeTab.value = tab;
  router.replace({ query: { ...route.query, tab } });
};
watch(() => route.query.tab, tab => {
  if (['available', 'active', 'history'].includes(tab)) activeTab.value = tab;
}, { immediate: true });

const loadProfile = async () => { const response = await request.get('/api/v1/riders/me'); profile.value = response.data; };
const loadTasks = async () => {
  const calls = [request.get('/api/v1/riders/me/tasks?active=true'), request.get('/api/v1/riders/me/tasks?active=false')];
  if (profile.value?.online) calls.push(request.get('/api/v1/riders/available-tasks'));
  const [active, history, available] = await Promise.all(calls);
  activeTasks.value = active.data || []; historyTasks.value = history.data || []; availableTasks.value = available?.data || [];
};
const refreshAll = async () => { refreshing.value = true; try { await loadProfile(); if(profile.value?.auditStatus === 1) await loadTasks(); } catch(e){ toast.error(e.response?.data?.message || '数据同步失败'); } finally { refreshing.value=false; } };
const toggleOnline = async () => { switching.value=true; try { const res=await request.patch('/api/v1/riders/me/online',{online:!profile.value.online}); profile.value=res.data; await loadTasks(); toast.success(profile.value.online?'已上线，可以接单了':'已安全下线'); } catch(e){toast.error(e.response?.data?.message||'状态更新失败');}finally{switching.value=false;} };
const act = async (task, action) => { actingId.value=task.id; try { await request.post(`/api/v1/delivery-tasks/${task.id}/${action}`); toast.success(action==='accept'?'抢单成功，请安全前往商家':'配送状态已更新'); activeTab.value='active'; await refreshAll(); } catch(e){toast.error(e.response?.data?.message||'操作失败');}finally{actingId.value=null;} };
const navigate = keyword => window.open(`https://uri.amap.com/search?keyword=${encodeURIComponent(keyword || '')}`,'_blank');
const openException = task => { exceptionModal.value=task; exceptionForm.exceptionType='STORE_DELAY'; exceptionForm.description=''; };
const submitException = async () => { if(!exceptionForm.description) return toast.warning('请填写情况说明'); try{await request.post(`/api/v1/delivery-tasks/${exceptionModal.value.id}/exceptions`,exceptionForm);toast.success('异常已上报，调度员将尽快处理');exceptionModal.value=null;await refreshAll();}catch(e){toast.error(e.response?.data?.message||'上报失败');} };
const initSocket = () => {
  if (!profile.value?.userId || destroyed) return;
  socket.value = new WebSocket(getWebSocketUrl(`/ws/${profile.value.userId}`));
  socket.value.onmessage = async event => {
    try {
      const message = JSON.parse(event.data);
      if (message.type === 'delivery_update') await refreshAll();
    } catch (error) {
      console.warn('配送消息解析失败', error);
    }
  };
  socket.value.onclose = () => {
    if (!destroyed) reconnectTimer = window.setTimeout(initSocket, 2500);
  };
};
onMounted(async()=>{try{await loadProfile();if(!profile.value){router.replace('/rider/apply');return;}if(profile.value.auditStatus===1){await loadTasks();initSocket();}}catch(e){toast.error(e.response?.data?.message||'加载失败');}finally{loading.value=false;}});
onUnmounted(()=>{destroyed=true;if(reconnectTimer)window.clearTimeout(reconnectTimer);if(socket.value)socket.value.close();});
</script>

<style scoped>
*{box-sizing:border-box}.rider-page{min-height:100vh;background:#f3f6f9;color:#17283b;padding-bottom:50px}.topbar{height:78px;padding:0 max(24px,calc((100% - 1180px)/2));display:flex;align-items:center;justify-content:space-between;background:#092f3c;color:#fff}.brand{display:flex;gap:12px;align-items:center}.brand .logo{width:38px;height:38px;border-radius:12px;display:grid;place-items:center;background:#20c997;color:#063d38}.brand b,.brand small{display:block}.brand small{color:#8eb1ba;font-size:10px;letter-spacing:1.5px;margin-top:3px}.top-actions{display:flex;align-items:center;gap:12px}.user-side{border:1px solid rgba(255,255,255,.2);background:transparent;color:#d9e8ec;border-radius:11px;padding:10px 14px;cursor:pointer}.avatar{width:38px;height:38px;border:0;border-radius:50%;background:#d8fff3;color:#067c66;font-weight:900}.hero-card,.stat-grid,.workspace,.loading-card{width:min(calc(100% - 40px),1180px);margin-left:auto;margin-right:auto}.hero-card{margin-top:30px;padding:30px 34px;border-radius:24px;background:radial-gradient(circle at 80% 0,rgba(102,229,196,.2),transparent 28%),linear-gradient(135deg,#0b4652,#0b6d69);color:#fff;display:flex;justify-content:space-between;align-items:center;box-shadow:0 20px 45px rgba(11,70,82,.18)}.date{color:#7fe4cb;font-size:12px;font-weight:800;letter-spacing:1px}.hero-card h1{font-size:30px;margin:6px 0 8px}.hero-note{color:#b7d4d6;font-size:14px}.online-switch{min-width:174px;padding:13px 17px;border:1px solid rgba(255,255,255,.17);background:rgba(0,0,0,.17);border-radius:16px;color:#fff;display:flex;gap:12px;align-items:center;cursor:pointer}.online-switch.online{background:rgba(47,221,170,.13);border-color:#44d8ae}.switch-dot{width:13px;height:13px;border-radius:50%;background:#7f9ba0;box-shadow:0 0 0 5px rgba(127,155,160,.15)}.online .switch-dot{background:#38e1ac;box-shadow:0 0 0 5px rgba(56,225,172,.17)}.online-switch b,.online-switch small{display:block;text-align:left}.online-switch small{color:#9dbabe;font-size:10px;margin-top:3px}.stat-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-top:18px}.stat-grid article{padding:19px;background:#fff;border:1px solid #eaf0f3;border-radius:17px;display:flex;align-items:center;gap:13px;box-shadow:0 8px 24px rgba(25,52,72,.04)}.stat-icon{width:42px;height:42px;border-radius:13px;display:grid;place-items:center}.green{background:#e8fbf5;color:#0b9f7e}.blue{background:#ebf4ff;color:#2477d4}.orange{background:#fff5e6;color:#e78a17}.purple{background:#f2edff;color:#7656d8}.stat-grid small,.stat-grid strong{display:block}.stat-grid small{font-size:11px;color:#8795a5}.stat-grid strong{font-size:21px;margin-top:4px}.stat-grid em{font-style:normal;font-size:11px;color:#728296}.workspace{margin-top:20px;background:#fff;border-radius:20px;border:1px solid #e7edf1;min-height:420px;overflow:hidden}.tabs{display:flex;align-items:center;border-bottom:1px solid #edf1f4;padding:0 20px}.tabs button{border:0;background:none;padding:20px 17px;color:#6f7e8e;cursor:pointer;font-weight:700}.tabs button i{margin-right:8px}.tabs button b{margin-left:7px;padding:2px 6px;border-radius:10px;background:#e7f8f3;color:#0d9a79;font-size:10px}.tabs button.active{color:#078d72;box-shadow:inset 0 -3px #14b893}.tabs .refresh{margin-left:auto;font-size:12px}.spin{animation:spin .8s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}.task-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px;padding:20px}.task-card{border:1px solid #e4ebef;border-radius:17px;padding:20px;transition:.2s}.task-card:hover{transform:translateY(-2px);box-shadow:0 15px 32px rgba(19,50,69,.08)}.task-head{display:flex;justify-content:space-between;gap:12px}.task-id{font-size:9px;letter-spacing:1.5px;color:#95a1ad}.task-head h3{font-size:18px;margin:6px 0}.status{height:fit-content;padding:6px 9px;border-radius:9px;background:#edf8f5;color:#08836c;font-size:11px;font-weight:800}.status.exception{background:#fff1ec;color:#d95d31}.status.completed,.status.cancelled{background:#f0f2f4;color:#7b8794}.route{display:flex;gap:13px;padding:15px 0}.route-mark{width:20px;display:flex;align-items:center;flex-direction:column;color:#0ba284}.route-mark span{width:1px;flex:1;min-height:25px;border-left:1px dashed #bed0d2;margin:4px}.route-mark i:last-child{color:#ff8a3d}.route-copy{display:grid;gap:17px;flex:1}.route-copy small{color:#9aa5b0;font-size:10px}.route-copy p{font-size:13px;margin:4px 0 0;line-height:1.45}.route-copy em{font-size:11px;color:#718090;font-style:normal}.task-meta{display:flex;gap:15px;padding:13px 0;border-top:1px dashed #dfe6e9;color:#718090;font-size:11px}.task-meta i{color:#0aa889;margin-right:4px}.task-actions{display:flex;flex-wrap:wrap;gap:8px;margin-top:10px}.task-actions button,.modal-actions button{height:38px;padding:0 14px;border-radius:10px;font-weight:800;cursor:pointer}.accept,.primary{border:0;background:#0caf8b;color:#fff;box-shadow:0 8px 17px rgba(12,175,139,.18);flex:1}.ghost{border:1px solid #dce5e8;background:#fff;color:#536577;flex:1}.ghost.danger,.exception-link{color:#d3593a}.exception-link{width:100%;border:0!important;background:none!important;box-shadow:none!important;font-size:10px}.waiting-confirm,.complete-note{width:100%;padding:11px;border-radius:10px;text-align:center;background:#eef9f6;color:#16866f;font-size:12px}.waiting-confirm.warning{background:#fff5eb;color:#c77424}.empty,.loading-card{padding:70px 24px;text-align:center;color:#8795a3}.empty span,.loading-card>i{width:70px;height:70px;border-radius:50%;background:#edf9f6;color:#0ba98a;display:grid;place-items:center;margin:0 auto 17px;font-size:25px}.empty h3,.loading-card h2{color:#34485d;margin-bottom:8px}.empty p,.loading-card p{font-size:13px}.primary.small,.loading-card .primary{display:inline-block;margin-top:18px;padding:11px 20px;border-radius:10px;flex:none}.spinner{display:inline-block;width:18px;height:18px;border:2px solid #d1e5df;border-top-color:#0aa787;border-radius:50%;animation:spin .8s linear infinite;margin-right:8px}.modal-mask{position:fixed;inset:0;background:rgba(3,24,31,.62);display:grid;place-items:center;padding:20px;z-index:2000}.modal{width:min(100%,460px);background:#fff;border-radius:22px;padding:30px}.modal-icon{width:52px;height:52px;border-radius:15px;display:grid;place-items:center;background:#fff1e8;color:#de672f;font-size:20px}.modal h2{margin:15px 0 8px}.modal>p{color:#7c8997;font-size:13px;line-height:1.6}.modal label{display:block;margin-top:18px;font-size:12px;font-weight:800;color:#405269}.modal select,.modal textarea{width:100%;border:1px solid #dae3e7;border-radius:11px;padding:12px;margin-top:7px;font:inherit}.modal textarea{height:100px;resize:vertical}.modal-actions{display:flex;gap:10px;margin-top:20px}
@media(max-width:800px){.topbar{padding:0 18px}.brand small,.user-side{display:none}.hero-card{align-items:flex-start;gap:22px;padding:24px;flex-direction:column}.online-switch{width:100%}.stat-grid{grid-template-columns:repeat(2,1fr)}.task-grid{grid-template-columns:1fr}.tabs{overflow-x:auto;padding:0 6px}.tabs button{white-space:nowrap;padding:17px 12px}.tabs .refresh{margin-left:0}.task-meta{flex-wrap:wrap}}

/* 与用户首页一致的克制蓝白工作台。功能状态用颜色表达，视觉不再依赖渐变和发光。 */
.rider-page{background:#f6f9fd;color:#253b55;padding-bottom:78px}
.topbar{height:64px;padding:0 max(20px,calc((100% - 1120px)/2));background:#fff;color:#173b62;border-bottom:1px solid #e5eef8;box-shadow:0 1px 6px rgba(40,92,145,.05)}
.brand{gap:10px}.brand .logo{width:34px;height:34px;border-radius:8px;background:#e7f4ff;color:#0097ff}.brand b{font-size:16px;font-weight:700}.brand small{color:#7890aa;font-size:10px;letter-spacing:.5px;margin-top:2px}
.top-actions{gap:10px}.user-side{border:1px solid #d5e7f8;background:#fff;color:#287bc0;border-radius:6px;padding:8px 12px}.avatar{width:34px;height:34px;background:#e8f4ff;color:#1778c1;border-radius:50%}
.hero-card,.stat-grid,.workspace,.loading-card{width:min(calc(100% - 32px),1120px)}
.hero-card{margin-top:18px;padding:22px 24px;border-radius:10px;background:#fff;color:#253b55;border:1px solid #dfeaf5;box-shadow:0 4px 14px rgba(51,101,150,.06)}
.date{color:#6f8aa6;font-size:12px;letter-spacing:.2px}.hero-card h1{font-size:24px;margin:6px 0}.hero-note{color:#70849a;font-size:13px}
.online-switch{min-width:150px;padding:10px 13px;border:1px solid #cfe2f4;background:#f5f9fd;border-radius:7px;color:#44617d}.online-switch.online{background:#edf7ff;border-color:#9dccf3}.switch-dot{width:10px;height:10px;background:#a9b9c9;box-shadow:none}.online .switch-dot{background:#0097ff;box-shadow:0 0 0 4px #d9efff}.online-switch small{color:#7991a8}
.stat-grid{gap:12px;margin-top:14px}.stat-grid article{padding:15px 16px;border:1px solid #e0ebf6;border-radius:9px;box-shadow:none}.stat-icon{width:36px;height:36px;border-radius:8px}.green{background:#e9f5ff;color:#1983c6}.blue{background:#e9f5ff;color:#1983c6}.orange{background:#fff6e9;color:#d98a29}.purple{background:#f1f5fb;color:#617fa1}.stat-grid small{color:#8396aa}.stat-grid strong{font-size:19px}
.workspace{margin-top:16px;border-radius:10px;border:1px solid #dfeaf5;box-shadow:0 4px 14px rgba(51,101,150,.04)}.tabs{padding:0 16px}.tabs button{padding:17px 14px;color:#7489a0}.tabs button b{background:#e8f4ff;color:#1479c3}.tabs button.active{color:#0097ff;box-shadow:inset 0 -2px #0097ff}.tabs .refresh{color:#6d8aa8}
.task-grid{gap:12px;padding:16px}.task-card{border:1px solid #e0ebf5;border-radius:10px;padding:17px;transition:box-shadow .18s ease}.task-card:hover{transform:none;box-shadow:0 7px 18px rgba(42,94,145,.08)}.task-head h3{font-size:16px}.status{padding:5px 8px;border-radius:6px;background:#eaf6ff;color:#1978bd}.route{padding:13px 0}.route-mark{color:#1687ca}.route-mark i:last-child{color:#ef902f}.route-copy p{font-size:12px}.task-meta{gap:12px}.task-meta i{color:#1687ca}.task-actions button,.modal-actions button{height:36px;border-radius:6px}.accept,.primary{background:#0097ff;box-shadow:none}.ghost{border-color:#d6e5f2;color:#51718f}.waiting-confirm,.complete-note{border-radius:6px;background:#edf7ff;color:#2475ad}.waiting-confirm.warning{background:#fff6e9;color:#be7927}.empty span,.loading-card>i{width:60px;height:60px;border-radius:50%;background:#eaf5ff;color:#1687ca}.empty h3,.loading-card h2{color:#38536f}.primary.small,.loading-card .primary{border-radius:6px}.spinner{border-color:#d6e9f8;border-top-color:#0097ff}.modal-mask{background:rgba(18,53,89,.42)}.modal{border-radius:10px;padding:24px}.modal-icon{border-radius:8px}
@media(max-width:800px){.hero-card{padding:20px}.stat-grid{gap:8px}.stat-grid article{padding:13px 12px}.workspace{margin-top:12px}.task-grid{padding:12px}.rider-page{padding-bottom:74px}}

/* 统计卡片也只使用蓝色层级，橙色仅保留给真正的异常提示 */
.rider-page .stat-icon.orange, .rider-page .stat-icon.purple { background: #edf7ff; color: #2b81bf; }
</style>
