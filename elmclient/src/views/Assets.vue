<template>
  <div class="assets-page">
    <header><button @click="$router.back()">‹</button><h1>钱包与优惠</h1></header>
    <main>
      <section class="balance-card"><div><span>账户余额</span><strong>¥{{ money(asset.balance) }}</strong></div><button @click="recharge">模拟充值</button></section>
      <section class="asset-grid"><div><b>{{ asset.points || 0 }}</b><span>积分</span></div><div><b>{{ asset.availableCoupons || 0 }}</b><span>可用优惠券</span></div><div><b>{{ asset.member ? '会员中' : '普通用户' }}</b><span>{{ asset.member ? '到期 ' + formatDate(asset.membershipExpire) : '95折会员' }}</span></div></section>
      <section class="coupon-card">
        <div class="coupon-heading"><div><h2>我的红包</h2><p>结算时会按门槛展示可用红包</p></div><button @click="claimCoupon" :disabled="claiming || welcomeClaimed">{{ claiming ? '领取中' : welcomeClaimed ? '新人券已领取' : '领取新人券' }}</button></div>
        <div v-if="coupons.length" class="coupon-list">
          <div v-for="coupon in coupons" :key="coupon.id" class="coupon-item"><strong><small>¥</small>{{ money(coupon.discountAmount) }}</strong><span><b>{{ coupon.name }}</b><small>满 ¥{{ money(coupon.minOrderAmount) }} 可用 · {{ formatDate(coupon.expiresAt) }} 到期</small></span></div>
        </div>
        <p v-else class="empty-coupon">暂无可用红包</p>
      </section>
      <section class="membership-card"><div><h2>连续包月会员</h2><p>开通后30天内享受订单商品95折（演示权益）</p></div><button @click="activateMember" :disabled="asset.member">{{ asset.member ? '已开通' : '开通会员' }}</button></section>
      <section class="stats-card"><h2>我的消费</h2><div class="stats-row"><span>已完成订单 <b>{{ spending.completedOrderCount || 0 }}</b></span><span>累计消费 <b>¥{{ money(spending.totalSpent) }}</b></span><span>常去店铺 <b>{{ spending.visitedBusinessCount || 0 }}</b></span></div></section>
      <section class="ledger-card"><h2>资产流水</h2><div v-if="!ledger.length" class="empty-ledger">暂无资产变更</div><div v-for="item in ledger" :key="item.id" class="ledger-item"><span>{{ ledgerLabel(item.type) }}<small>{{ item.reason }}</small></span><b :class="{ positive: item.amount > 0 || item.pointsDelta > 0 }">{{ item.amount > 0 ? '+' + money(item.amount) + '元' : item.pointsDelta > 0 ? '+' + item.pointsDelta + '分' : '已记录' }}</b></div></section>
      <p class="hint">资产数据与账号绑定，订单完成后积分自动累计；充值为课堂演示用模拟操作。</p>
    </main>
  </div>
</template>
<script setup>
import { computed, ref, onMounted } from 'vue';
import request from '../utils/request';
import { toast } from '../utils/toast';
const asset = ref({ balance: 0, points: 0, availableCoupons: 0, member: false }); const claiming = ref(false);
const spending = ref({ completedOrderCount: 0, totalSpent: 0, visitedBusinessCount: 0 });
const ledger = ref([]);
const coupons = ref([]);
const welcomeClaimed = computed(() => Boolean(asset.value.welcomeCouponClaimed)
  || coupons.value.some(coupon => coupon.name === '新人券'));
const load = async () => { const [assetRes, statsRes, ledgerRes, couponRes] = await Promise.all([request.get('/api/v1/assets/me'), request.get('/api/v1/assets/spending-stats'), request.get('/api/v1/assets/ledger'), request.get('/api/v1/assets/coupons')]); if (assetRes.success) asset.value = assetRes.data || asset.value; if (statsRes.success) spending.value = statsRes.data || spending.value; if (ledgerRes.success) ledger.value = ledgerRes.data || []; if (couponRes.success) coupons.value = couponRes.data || []; };
const money = (v) => Number(v || 0).toFixed(2); const formatDate = (v) => v ? new Date(v).toLocaleDateString('zh-CN') : '-';
const recharge = async () => { const amount = window.prompt('输入充值金额（1-500元）', '20'); if (!amount) return; try { const res=await request.post('/api/v1/assets/recharge', null, { params: { amount } }); if(res.success){await load();toast.success('充值成功');} } catch(e){toast.error(e?.message || '充值失败');} };
const claimCoupon = async () => { if(welcomeClaimed.value) return toast.info('新人券每个账号只能领取一次'); claiming.value=true; try { const res=await request.post('/api/v1/assets/welcome-coupon'); if(res.success){asset.value=res.data || asset.value;await load();toast.success('优惠券已放入卡包');} } catch(e){toast.warning(e?.response?.data?.message || e?.message || '领取失败');await load();} finally {claiming.value=false;} };
const activateMember = async () => { const res=await request.post('/api/v1/assets/membership'); if(res.success){await load();toast.success('会员已开通，有效期30天');} };
const ledgerLabel = (type) => ({ RECHARGE:'充值', COUPON_GRANT:'优惠券', MEMBERSHIP:'会员', POINT_EARN:'积分' }[type] || '资产变更');
onMounted(load);
</script>
<style scoped>
.assets-page{min-height:100vh;background:#f5f8fc;color:#29445d}header{height:56px;background:#168bd1;color:#fff;display:flex;align-items:center;padding:0 16px;gap:12px;position:sticky;top:0;z-index:2}header button{border:0;background:none;color:#fff;font-size:30px;line-height:1}h1{font-size:18px;margin:0}main{max-width:640px;margin:auto;padding:20px 16px}.balance-card,.coupon-card,.membership-card{background:#fff;border:1px solid #e0ebf4;border-radius:12px;padding:20px;box-shadow:0 3px 12px rgba(45,95,130,.06)}.balance-card,.membership-card{display:flex;justify-content:space-between;align-items:center}.balance-card span,.asset-grid span{display:block;color:#7d93a6;font-size:13px}.balance-card strong{display:block;font-size:32px;color:#168bd1;margin-top:8px}.balance-card button,.coupon-card button,.membership-card button{border:0;background:#168bd1;color:#fff;border-radius:7px;padding:9px 14px}.asset-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;margin:14px 0}.asset-grid>div{background:#fff;border:1px solid #e0ebf4;border-radius:10px;padding:16px 10px;text-align:center}.asset-grid b{display:block;font-size:20px;color:#2f628a;margin-bottom:6px}.coupon-card h2,.membership-card h2{font-size:16px;margin:0 0 7px}.coupon-card p,.membership-card p{color:#8094a5;font-size:12px;margin:0}.coupon-card button:disabled,.membership-card button:disabled{opacity:.6}.coupon-heading{display:flex;justify-content:space-between;align-items:center;gap:12px}.coupon-list{margin-top:14px;border-top:1px solid #edf3f7}.coupon-item{display:flex;align-items:center;gap:14px;padding:12px 0;border-bottom:1px solid #edf3f7}.coupon-item:last-child{padding-bottom:0;border-bottom:0}.coupon-item>strong{min-width:66px;color:#168bd1;font-size:23px}.coupon-item>strong small{font-size:12px;margin-right:2px}.coupon-item span{min-width:0}.coupon-item span b,.coupon-item span small{display:block}.coupon-item span b{font-size:13px;color:#29445d;margin-bottom:4px}.coupon-item span small{font-size:11px;color:#8aa0b2;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.empty-coupon{padding-top:14px!important;margin-top:14px!important;border-top:1px solid #edf3f7}.membership-card{margin-top:12px}.hint{font-size:12px;color:#8aa0b2;line-height:1.7;margin:18px 4px}@media(max-width:375px){.asset-grid b{font-size:16px}.balance-card strong{font-size:26px}.coupon-heading{align-items:flex-start;flex-direction:column}.coupon-heading button{width:100%}}
.stats-card{background:#fff;border:1px solid #e0ebf4;border-radius:12px;padding:18px 20px;margin-top:12px;box-shadow:0 3px 12px rgba(45,95,130,.06)}.stats-card h2{font-size:16px;margin:0 0 10px}.stats-row{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;color:#7d93a6;font-size:12px;text-align:center}.stats-row b{display:block;color:#2f628a;font-size:18px;margin-top:5px}
.ledger-card{background:#fff;border:1px solid #e0ebf4;border-radius:12px;padding:18px 20px;margin-top:12px;box-shadow:0 3px 12px rgba(45,95,130,.06)}.ledger-card h2{font-size:16px;margin:0 0 10px}.ledger-item{display:flex;justify-content:space-between;align-items:center;padding:9px 0;border-bottom:1px solid #eef3f7;font-size:13px}.ledger-item:last-child{border-bottom:0}.ledger-item small{display:block;color:#94a6b4;font-size:11px;margin-top:3px}.ledger-item b{color:#7f94a4;font-size:12px}.ledger-item b.positive{color:#168bd1}.empty-ledger{font-size:12px;color:#8aa0b2}
</style>
