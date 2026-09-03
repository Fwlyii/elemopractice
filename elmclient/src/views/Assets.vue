<template>
  <div class="assets-page">
    <header><button @click="$router.back()">‹</button><h1>钱包与优惠</h1></header>
    <main>
      <section class="balance-card"><div><span>账户余额</span><strong>¥{{ money(asset.balance) }}</strong></div><button @click="recharge">模拟充值</button></section>
      <section class="asset-grid"><div><b>{{ asset.points || 0 }}</b><span>积分</span></div><div><b>{{ asset.availableCoupons || 0 }}</b><span>可用优惠券</span></div><div><b>{{ asset.member ? '会员中' : '普通用户' }}</b><span>{{ asset.member ? '到期 ' + formatDate(asset.membershipExpire) : '95折会员' }}</span></div></section>
      <section class="coupon-card"><div><h2>新人专享券</h2><p>满20减5 · 有效期30天 · 每人限领一次</p></div><button @click="claimCoupon" :disabled="claiming">{{ claiming ? '领取中' : '立即领取' }}</button></section>
      <section class="membership-card"><div><h2>连续包月会员</h2><p>开通后30天内享受订单商品95折（演示权益）</p></div><button @click="activateMember" :disabled="asset.member">{{ asset.member ? '已开通' : '开通会员' }}</button></section>
      <section class="stats-card"><h2>我的消费</h2><div class="stats-row"><span>已完成订单 <b>{{ spending.completedOrderCount || 0 }}</b></span><span>累计消费 <b>¥{{ money(spending.totalSpent) }}</b></span><span>常去店铺 <b>{{ spending.visitedBusinessCount || 0 }}</b></span></div></section>
      <section class="ledger-card"><h2>资产流水</h2><div v-if="!ledger.length" class="empty-ledger">暂无资产变更</div><div v-for="item in ledger" :key="item.id" class="ledger-item"><span>{{ ledgerLabel(item.type) }}<small>{{ item.reason }}</small></span><b :class="{ positive: item.amount > 0 || item.pointsDelta > 0 }">{{ item.amount > 0 ? '+' + money(item.amount) + '元' : item.pointsDelta > 0 ? '+' + item.pointsDelta + '分' : '已记录' }}</b></div></section>
      <p class="hint">资产数据与账号绑定，订单完成后积分自动累计；充值为课堂演示用模拟操作。</p>
    </main>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue';
import request from '../utils/request';
import { toast } from '../utils/toast';
const asset = ref({ balance: 0, points: 0, availableCoupons: 0, member: false }); const claiming = ref(false);
const spending = ref({ completedOrderCount: 0, totalSpent: 0, visitedBusinessCount: 0 });
const ledger = ref([]);
const load = async () => { const [assetRes, statsRes, ledgerRes] = await Promise.all([request.get('/api/v1/assets/me'), request.get('/api/v1/assets/spending-stats'), request.get('/api/v1/assets/ledger')]); if (assetRes.success) asset.value = assetRes.data || asset.value; if (statsRes.success) spending.value = statsRes.data || spending.value; if (ledgerRes.success) ledger.value = ledgerRes.data || []; };
const money = (v) => Number(v || 0).toFixed(2); const formatDate = (v) => v ? new Date(v).toLocaleDateString('zh-CN') : '-';
const recharge = async () => { const amount = window.prompt('输入充值金额（1-500元）', '20'); if (!amount) return; try { const res=await request.post('/api/v1/assets/recharge', null, { params: { amount } }); if(res.success){await load();toast.success('充值成功');} } catch(e){toast.error(e?.message || '充值失败');} };
const claimCoupon = async () => { claiming.value=true; try { const res=await request.post('/api/v1/assets/welcome-coupon'); if(res.success){await load();toast.success('优惠券已放入卡包');} } finally {claiming.value=false;} };
const activateMember = async () => { const res=await request.post('/api/v1/assets/membership'); if(res.success){await load();toast.success('会员已开通，有效期30天');} };
const ledgerLabel = (type) => ({ RECHARGE:'充值', COUPON_GRANT:'优惠券', MEMBERSHIP:'会员', POINT_EARN:'积分' }[type] || '资产变更');
onMounted(load);
</script>
<style scoped>
.assets-page{min-height:100vh;background:#f5f8fc;color:#29445d}header{height:56px;background:#168bd1;color:#fff;display:flex;align-items:center;padding:0 16px;gap:12px;position:sticky;top:0;z-index:2}header button{border:0;background:none;color:#fff;font-size:30px;line-height:1}h1{font-size:18px;margin:0}main{max-width:640px;margin:auto;padding:20px 16px}.balance-card,.coupon-card,.membership-card{background:#fff;border:1px solid #e0ebf4;border-radius:12px;padding:20px;display:flex;justify-content:space-between;align-items:center;box-shadow:0 3px 12px rgba(45,95,130,.06)}.balance-card span,.asset-grid span{display:block;color:#7d93a6;font-size:13px}.balance-card strong{display:block;font-size:32px;color:#168bd1;margin-top:8px}.balance-card button,.coupon-card button,.membership-card button{border:0;background:#168bd1;color:#fff;border-radius:7px;padding:9px 14px}.asset-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;margin:14px 0}.asset-grid>div{background:#fff;border:1px solid #e0ebf4;border-radius:10px;padding:16px 10px;text-align:center}.asset-grid b{display:block;font-size:20px;color:#2f628a;margin-bottom:6px}.coupon-card h2,.membership-card h2{font-size:16px;margin:0 0 7px}.coupon-card p,.membership-card p{color:#8094a5;font-size:12px;margin:0}.coupon-card button:disabled,.membership-card button:disabled{opacity:.6}.membership-card{margin-top:12px}.hint{font-size:12px;color:#8aa0b2;line-height:1.7;margin:18px 4px}@media(max-width:375px){.asset-grid b{font-size:16px}.balance-card strong{font-size:26px}}
.stats-card{background:#fff;border:1px solid #e0ebf4;border-radius:12px;padding:18px 20px;margin-top:12px;box-shadow:0 3px 12px rgba(45,95,130,.06)}.stats-card h2{font-size:16px;margin:0 0 10px}.stats-row{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;color:#7d93a6;font-size:12px;text-align:center}.stats-row b{display:block;color:#2f628a;font-size:18px;margin-top:5px}
.ledger-card{background:#fff;border:1px solid #e0ebf4;border-radius:12px;padding:18px 20px;margin-top:12px;box-shadow:0 3px 12px rgba(45,95,130,.06)}.ledger-card h2{font-size:16px;margin:0 0 10px}.ledger-item{display:flex;justify-content:space-between;align-items:center;padding:9px 0;border-bottom:1px solid #eef3f7;font-size:13px}.ledger-item:last-child{border-bottom:0}.ledger-item small{display:block;color:#94a6b4;font-size:11px;margin-top:3px}.ledger-item b{color:#7f94a4;font-size:12px}.ledger-item b.positive{color:#168bd1}.empty-ledger{font-size:12px;color:#8aa0b2}
</style>
