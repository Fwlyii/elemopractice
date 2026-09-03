<template>
  <main class="admin-page">
    <header class="page-head">
      <div><p>平台管理</p><h1>骑手与配送调度</h1><span>审核骑手资质，处理配送异常</span></div>
      <button @click="loadAll"><i class="fas fa-sync-alt" :class="{spin:loading}"></i> 刷新数据</button>
    </header>
    <section class="kpis">
      <article><i class="fas fa-user-clock amber"></i><div><small>待审核申请</small><b>{{ pendingApplications.length }}</b></div></article>
      <article><i class="fas fa-motorcycle green"></i><div><small>已认证骑手</small><b>{{ approvedCount }}</b></div></article>
      <article><i class="fas fa-satellite-dish red"></i><div><small>待处理异常</small><b>{{ openExceptions.length }}</b></div></article>
      <article><i class="fas fa-shield-alt blue"></i><div><small>已闭环异常</small><b>{{ resolvedCount }}</b></div></article>
    </section>

    <section class="panel">
      <nav><button :class="{active:tab==='applications'}" @click="tab='applications'">骑手资质审核 <b>{{ pendingApplications.length }}</b></button><button :class="{active:tab==='exceptions'}" @click="tab='exceptions'">配送异常工单 <b>{{ openExceptions.length }}</b></button></nav>
      <div v-if="loading" class="empty">正在同步调度数据…</div>

      <template v-else-if="tab==='applications'">
        <div class="toolbar"><h2>申请列表</h2><select v-model="applicationFilter"><option value="all">全部状态</option><option value="0">待审核</option><option value="1">已通过</option><option value="2">已拒绝</option></select></div>
        <div v-if="filteredApplications.length===0" class="empty"><i class="fas fa-clipboard-check"></i><p>当前没有匹配的骑手申请</p></div>
        <div v-else class="table-wrap"><table><thead><tr><th>申请人</th><th>联系方式</th><th>配送方式</th><th>申请时间</th><th>状态</th><th>操作</th></tr></thead><tbody>
          <tr v-for="item in filteredApplications" :key="item.id"><td data-label="申请人"><div class="person"><span>{{ item.realName?.slice(0,1) }}</span><div><b>{{ item.realName }}</b><small>@{{ item.username }}</small></div></div></td><td data-label="联系方式">{{ item.phone }}</td><td data-label="配送方式">{{ vehicleName(item.vehicleType) }}</td><td data-label="申请时间">{{ formatTime(item.createTime) }}</td><td data-label="状态"><span class="pill" :class="'audit-'+item.auditStatus">{{ auditName(item.auditStatus) }}</span></td><td data-label="操作"><div v-if="item.auditStatus===0" class="row-actions"><button class="pass" @click="audit(item,true)">通过</button><button class="reject" @click="openReject(item)">拒绝</button></div><span v-else class="muted">{{ item.rejectReason || '已完成审核' }}</span></td></tr>
        </tbody></table></div>
      </template>

      <template v-else>
        <div class="toolbar"><h2>异常工单</h2><select v-model="exceptionFilter"><option value="all">全部状态</option><option value="0">待处理</option><option value="1">已闭环</option></select></div>
        <div v-if="filteredExceptions.length===0" class="empty"><i class="fas fa-shield-check"></i><p>当前没有配送异常</p></div>
        <div v-else class="exception-list"><article v-for="item in filteredExceptions" :key="item.id">
          <div class="exception-top"><span class="alert-icon"><i class="fas fa-exclamation"></i></span><div><small>INCIDENT #{{ item.id }} · 订单 #{{ item.orderId }}</small><h3>{{ exceptionName(item.exceptionType) }}</h3></div><span class="pill" :class="item.status===0?'audit-2':'audit-1'">{{ item.status===0?'待响应':'已闭环' }}</span></div>
          <p class="description">{{ item.description }}</p><div class="exception-meta"><span><i class="fas fa-motorcycle"></i> {{ item.riderName || '未知骑手' }}</span><span><i class="fas fa-store"></i> {{ item.businessName }}</span><span><i class="fas fa-clock"></i> {{ formatTime(item.createTime) }}</span></div>
          <div v-if="item.status===0" class="resolution-actions"><button @click="openResolution(item,'RESUME')"><i class="fas fa-play"></i> 恢复配送</button><button @click="openResolution(item,'REASSIGN')"><i class="fas fa-random"></i> 重新派单</button><button class="cancel" @click="openResolution(item,'CANCEL')"><i class="fas fa-ban"></i> 取消订单</button></div>
          <div v-else class="resolution-note"><b>{{ actionName(item.resolutionAction) }}</b>{{ item.resolutionNote || '异常已处理' }}</div>
        </article></div>
      </template>
    </section>

    <div v-if="modal" class="modal-mask" @click.self="modal=null"><form class="modal" @submit.prevent="submitModal"><span class="modal-symbol"><i :class="modal.type==='reject'?'fas fa-user-times':'fas fa-headset'"></i></span><h2>{{ modalTitle }}</h2><p>{{ modalDescription }}</p><textarea v-model.trim="modal.note" maxlength="500" :placeholder="modal.type==='reject'?'请填写拒绝原因（必填）':'填写调度处理说明'" :required="modal.type==='reject'"></textarea><div><button type="button" class="secondary" @click="modal=null">返回</button><button type="submit" class="confirm">确认处理</button></div></form></div>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';import request from '@/utils/request';import { toast } from '@/utils/toast';
const applications=ref([]),exceptions=ref([]),loading=ref(false),tab=ref('applications'),applicationFilter=ref('all'),exceptionFilter=ref('all'),modal=ref(null);
const pendingApplications=computed(()=>applications.value.filter(x=>x.auditStatus===0));const approvedCount=computed(()=>applications.value.filter(x=>x.auditStatus===1).length);const openExceptions=computed(()=>exceptions.value.filter(x=>x.status===0));const resolvedCount=computed(()=>exceptions.value.filter(x=>x.status===1).length);
const filteredApplications=computed(()=>applicationFilter.value==='all'?applications.value:applications.value.filter(x=>x.auditStatus===Number(applicationFilter.value)));const filteredExceptions=computed(()=>exceptionFilter.value==='all'?exceptions.value:exceptions.value.filter(x=>x.status===Number(exceptionFilter.value)));
const loadAll=async()=>{loading.value=true;try{const[a,e]=await Promise.all([request.get('/api/v1/admin/rider-applications'),request.get('/api/v1/admin/delivery-exceptions')]);applications.value=a.data||[];exceptions.value=e.data||[];}catch(err){toast.error(err.response?.data?.message||'调度数据加载失败');}finally{loading.value=false;}};
const audit=async(item,approved)=>{try{await request.post(`/api/v1/admin/rider-applications/${item.id}/audit`,{approved,reason:null});toast.success(`已通过 ${item.realName} 的骑手申请`);await loadAll();}catch(e){toast.error(e.response?.data?.message||'审核失败');}};const openReject=item=>modal.value={type:'reject',item,note:''};const openResolution=(item,action)=>modal.value={type:'resolution',item,action,note:''};
const modalTitle=computed(()=>modal.value?.type==='reject'?'拒绝骑手申请':actionName(modal.value?.action));const modalDescription=computed(()=>modal.value?.type==='reject'?`将退回 ${modal.value.item.realName} 的申请，对方可根据意见重新提交。`:`正在处理订单 #${modal.value?.item.orderId} 的配送异常，操作将写入履约轨迹。`);
const submitModal=async()=>{if(modal.value.type==='reject'){if(!modal.value.note)return toast.warning('请填写拒绝原因');try{await request.post(`/api/v1/admin/rider-applications/${modal.value.item.id}/audit`,{approved:false,reason:modal.value.note});toast.success('审核意见已发送');}catch(e){return toast.error(e.response?.data?.message||'审核失败');}}else{try{await request.post(`/api/v1/admin/delivery-exceptions/${modal.value.item.id}/resolve`,{action:modal.value.action,note:modal.value.note});toast.success('配送异常已处理并记录');}catch(e){return toast.error(e.response?.data?.message||'处理失败');}}modal.value=null;await loadAll();};
const vehicleName=v=>({E_BIKE:'电动车',BIKE:'自行车',WALK:'步行'}[v]||v);const auditName=v=>['待审核','已通过','已拒绝'][v]||'未知';const exceptionName=v=>({STORE_DELAY:'商家出餐延迟',CUSTOMER_UNREACHABLE:'无法联系顾客',ADDRESS_ERROR:'收货地址异常',VEHICLE_FAILURE:'骑手车辆故障',OTHER:'其他配送问题'}[v]||v);const actionName=v=>({RESUME:'恢复原配送',REASSIGN:'重新分配骑手',CANCEL:'取消订单'}[v]||'处理配送异常');const formatTime=v=>v?new Date(v).toLocaleString('zh-CN'):'-';onMounted(loadAll);
</script>

<style scoped>
*{box-sizing:border-box}.admin-page{min-height:100vh;background:#f3f6fa;padding:34px max(24px,calc((100% - 1240px)/2)) 100px;color:#1b2a3a}.page-head{padding:32px 35px;border-radius:22px;color:#fff;background:radial-gradient(circle at 83% 20%,rgba(79,209,167,.22),transparent 25%),linear-gradient(130deg,#112f42,#0b5a61);display:flex;justify-content:space-between;align-items:center}.page-head p{font-size:10px;color:#55d9b7;letter-spacing:2px;font-weight:800}.page-head h1{font-size:28px;margin:7px 0}.page-head span{font-size:13px;color:#b5cdd2}.page-head button{border:1px solid rgba(255,255,255,.23);background:rgba(255,255,255,.08);color:#fff;border-radius:12px;padding:12px 17px;cursor:pointer}.kpis{display:grid;grid-template-columns:repeat(4,1fr);gap:15px;margin:18px 0}.kpis article{background:#fff;padding:18px;border-radius:16px;border:1px solid #e9eef3;display:flex;align-items:center;gap:14px}.kpis i{width:42px;height:42px;border-radius:13px;display:grid;place-items:center}.kpis .amber{background:#fff5df;color:#d98b16}.kpis .green{background:#e6faf4;color:#08a17f}.kpis .red{background:#fff0ed;color:#d95c43}.kpis .blue{background:#eaf3ff;color:#2878d2}.kpis small,.kpis b{display:block}.kpis small{color:#8290a0;font-size:11px}.kpis b{font-size:24px;margin-top:3px}.panel{background:#fff;border:1px solid #e4ebf0;border-radius:19px;min-height:470px;overflow:hidden}.panel>nav{border-bottom:1px solid #e8edf1;display:flex;padding:0 22px}.panel>nav button{padding:20px 17px;border:0;background:transparent;color:#718090;font-weight:800;cursor:pointer}.panel>nav button.active{color:#07896f;box-shadow:inset 0 -3px #0cae8a}.panel>nav b{background:#e9f8f4;color:#07896f;padding:2px 6px;border-radius:10px;font-size:10px}.toolbar{display:flex;align-items:center;justify-content:space-between;padding:22px 28px}.toolbar h2{font-size:18px}.toolbar select{border:1px solid #dce4ea;border-radius:9px;padding:8px 12px;background:#fff;color:#566678}.table-wrap{overflow-x:auto;padding:0 20px 25px}table{width:100%;border-collapse:collapse}th{text-align:left;padding:12px;color:#92a0ad;font-size:10px;border-bottom:1px solid #edf1f4}td{padding:15px 12px;border-bottom:1px solid #f0f3f5;font-size:12px}.person{display:flex;gap:10px;align-items:center}.person>span{width:34px;height:34px;border-radius:10px;background:#e4f8f2;color:#07856c;display:grid;place-items:center;font-weight:900}.person b,.person small{display:block}.person small{color:#9aa6b2;margin-top:3px}.pill{padding:6px 9px;border-radius:12px;font-size:10px;font-weight:800}.audit-0{background:#fff6dd;color:#b4770b}.audit-1{background:#e6f9f3;color:#078369}.audit-2{background:#fff0ed;color:#cf543b}.row-actions{display:flex;gap:6px}.row-actions button{padding:7px 10px;border-radius:8px;font-size:10px;font-weight:800;cursor:pointer}.pass{background:#0ca989;color:#fff;border:0}.reject{background:#fff;border:1px solid #efc7bd;color:#c8543a}.muted{color:#9aa4af;font-size:10px}.empty{text-align:center;padding:80px;color:#8c99a6}.empty i{font-size:28px;color:#0eaa89;margin-bottom:10px}.exception-list{padding:0 24px 25px;display:grid;gap:13px}.exception-list article{border:1px solid #e4eaee;border-radius:15px;padding:18px}.exception-top{display:flex;align-items:center;gap:12px}.exception-top>.pill{margin-left:auto}.alert-icon{width:40px;height:40px;border-radius:12px;background:#fff0eb;color:#d75c35;display:grid;place-items:center}.exception-top small{font-size:9px;letter-spacing:1px;color:#9ba6b0}.exception-top h3{font-size:15px;margin-top:4px}.description{margin:14px 0;padding:12px;background:#f8fafb;border-radius:9px;font-size:12px;color:#586979}.exception-meta{display:flex;gap:24px;color:#7b8997;font-size:11px}.exception-meta i{color:#0aa686;margin-right:5px}.resolution-actions{display:flex;gap:8px;margin-top:15px;padding-top:13px;border-top:1px dashed #dce4e8}.resolution-actions button{border:1px solid #dce5e8;background:#fff;color:#536577;padding:8px 11px;border-radius:8px;font-size:11px;cursor:pointer}.resolution-actions button:first-child{background:#0ca889;color:#fff;border-color:#0ca889}.resolution-actions .cancel{color:#c8553e}.resolution-note{margin-top:13px;padding:10px;background:#edf8f5;color:#568176;font-size:11px;border-radius:8px}.resolution-note b{margin-right:12px;color:#07846b}.modal-mask{position:fixed;inset:0;background:rgba(6,25,34,.6);display:grid;place-items:center;padding:20px;z-index:3000}.modal{width:min(100%,440px);background:#fff;border-radius:20px;padding:28px}.modal-symbol{width:48px;height:48px;border-radius:14px;background:#fff0eb;color:#d55d3c;display:grid;place-items:center}.modal h2{font-size:20px;margin:15px 0 7px}.modal p{color:#7a8896;font-size:12px;line-height:1.6}.modal textarea{width:100%;height:100px;margin:18px 0;border:1px solid #dce4e8;border-radius:11px;padding:12px;resize:vertical}.modal>div{display:flex;gap:9px}.modal button{height:41px;flex:1;border-radius:10px;font-weight:800;cursor:pointer}.secondary{border:1px solid #dce4e8;background:#fff;color:#627282}.confirm{border:0;background:#0ca889;color:#fff}.spin{animation:spin .8s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}
@media(max-width:800px){.admin-page{padding:18px 12px 90px}.page-head{align-items:flex-start;padding:25px;gap:15px}.page-head h1{font-size:22px}.page-head span,.page-head button{display:none}.kpis{grid-template-columns:repeat(2,1fr)}.kpis article{padding:13px}.panel>nav{overflow:auto;padding:0 5px}.panel>nav button{white-space:nowrap}.exception-meta{flex-direction:column;gap:7px}.resolution-actions{flex-wrap:wrap}}

/* 调度端与其余工作台使用同一套蓝白视觉 */
.admin-page { background: #f5f9fd; color: #24405c; }
.page-head { padding: 24px 28px; border-radius: 10px; color: #173b60; background: #eaf5ff; border: 1px solid #d5e9f8; }
.page-head p { color: #4b86ae; letter-spacing: 1px; }
.page-head h1 { color: #173b60; }
.page-head span { color: #637f97; }
.page-head button { border: 1px solid #b9d9ef; background: #fff; color: #2878ad; border-radius: 7px; }
.kpis { gap: 12px; }
.kpis article { border: 1px solid #e1edf7; border-radius: 9px; box-shadow: none; }
.kpis .amber, .kpis .green, .kpis .red, .kpis .blue { background: #eaf5ff; color: #1683c8; }
.panel { border: 1px solid #dfeaf5; border-radius: 10px; box-shadow: none; }
.panel>nav button.active { color: #0879c7; box-shadow: inset 0 -3px #0097ff; }
.panel>nav b { background: #e8f4ff; color: #1479c3; }
.toolbar select { border-color: #cfdfeb; border-radius: 6px; }
.table-wrap { padding-left: 16px; padding-right: 16px; }
.person>span { background: #eaf5ff; color: #1683c8; }
.audit-0 { background: #fff6e9; color: #b4770b; }
.audit-1 { background: #eaf5ff; color: #1479c3; }
.pass { background: #0097ff; }
.exception-list article { border-color: #e1edf7; border-radius: 9px; }
.alert-icon { background: #fff6e9; color: #c77b22; }
.exception-meta i { color: #1683c8; }
.resolution-actions button:first-child, .confirm { background: #0097ff; border-color: #0097ff; }
.resolution-note { background: #edf7ff; color: #547898; }
.resolution-note b { color: #1479c3; }
@media(max-width:800px){.page-head{padding:20px}.kpis article{padding:12px}.panel>nav button{padding:16px 12px}}
@media(max-width:800px){
  .table-wrap { overflow: visible; padding: 0 12px 20px; }
  .table-wrap table { display: block; }
  .table-wrap thead { display: none; }
  .table-wrap tbody { display: grid; gap: 10px; }
  .table-wrap tr { display: grid; grid-template-columns: minmax(0,1fr) auto; gap: 9px 14px; padding: 14px; background: #fff; border: 1px solid #e1edf7; border-radius: 8px; }
  .table-wrap td { display: block; min-width: 0; padding: 0; border: 0; font-size: 12px; }
  .table-wrap td::before { content: attr(data-label); display: block; margin-bottom: 3px; color: #91a2b1; font-size: 10px; }
  .table-wrap td:first-child { grid-row: span 4; align-self: start; }
  .table-wrap td:first-child::before { display: none; }
  .table-wrap td:nth-child(6) { align-self: end; }
  .table-wrap .person { gap: 8px; }
  .table-wrap .row-actions { justify-content: flex-end; }
}
</style>
