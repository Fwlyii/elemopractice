<template>
  <main class="apply-page">
    <section class="hero">
      <button class="back" @click="$router.push('/index')">← 返回首页</button>
      <div class="hero-icon"><i class="fas fa-motorcycle"></i></div>
      <p class="eyebrow">ELEM 即时配送网络</p>
      <h1>把每一份热乎，准时送达</h1>
      <p class="subtitle">简单资质审核，灵活上线接单，配送轨迹全程可见。</p>
      <div class="hero-points">
        <span><b>01</b> 提交申请</span><span><b>02</b> 管理员审核</span><span><b>03</b> 上线接单</span>
      </div>
    </section>

    <section class="form-panel">
      <div v-if="loading" class="state-card">正在读取骑手档案…</div>

      <div v-else-if="profile && profile.auditStatus === 0" class="state-card pending">
        <div class="state-icon"><i class="fas fa-hourglass-half"></i></div>
        <span class="status-pill">审核中</span>
        <h2>申请已进入审核队列</h2>
        <p>我们已收到 <b>{{ profile.realName }}</b> 的申请。审核结果会通过站内消息通知，无需重复提交。</p>
        <div class="profile-preview">
          <span>联系电话 <b>{{ maskPhone(profile.phone) }}</b></span>
          <span>配送方式 <b>{{ vehicleName(profile.vehicleType) }}</b></span>
        </div>
        <button class="secondary" @click="$router.push('/index')">返回首页</button>
      </div>

      <div v-else-if="profile && profile.auditStatus === 1" class="state-card approved">
        <div class="state-icon"><i class="fas fa-check"></i></div>
        <span class="status-pill">已通过</span>
        <h2>骑手资质已生效</h2>
        <p>身份审核已通过，可以进入工作台上线接单。</p>
        <button class="primary" @click="$router.push('/rider/dashboard')">进入骑手工作台 →</button>
      </div>

      <form v-else @submit.prevent="submit">
        <div class="panel-heading">
          <div>
            <p class="step">骑手入驻·基础资质</p>
            <h2>{{ profile?.auditStatus === 2 ? '重新提交申请' : '申请成为配送骑手' }}</h2>
          </div>
          <span v-if="profile?.auditStatus === 2" class="rejected">上次未通过</span>
        </div>
        <p v-if="profile?.rejectReason" class="reject-note">审核意见：{{ profile.rejectReason }}</p>

        <label>
          <span>真实姓名</span>
          <input v-model.trim="form.realName" maxlength="50" placeholder="请填写本人姓名" required>
        </label>
        <label>
          <span>手机号码</span>
          <input v-model.trim="form.phone" inputmode="numeric" maxlength="11" placeholder="11 位手机号" required>
        </label>
        <fieldset>
          <legend>配送方式</legend>
          <div class="vehicle-grid">
            <button v-for="item in vehicles" :key="item.value" type="button"
              :class="{ selected: form.vehicleType === item.value }" @click="form.vehicleType = item.value">
              <i :class="item.icon"></i><span>{{ item.label }}</span><small>{{ item.note }}</small>
            </button>
          </div>
        </fieldset>
        <label class="agreement">
          <input v-model="agreed" type="checkbox">
          <span>我承诺填写信息真实，并遵守平台配送安全规范</span>
        </label>
        <button class="primary" type="submit" :disabled="submitting || !agreed">
          {{ submitting ? '正在提交…' : '提交资质审核' }}
        </button>
        <p class="privacy"><i class="fas fa-shield-alt"></i> 信息仅用于骑手资质审核与配送联系</p>
      </form>
    </section>
  </main>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import request from '@/utils/request';
import { toast } from '@/utils/toast';

const router = useRouter();
const loading = ref(true);
const submitting = ref(false);
const agreed = ref(false);
const profile = ref(null);
const form = reactive({ realName: '', phone: '', vehicleType: 'E_BIKE' });
const vehicles = [
  { value: 'E_BIKE', label: '电动车', note: '快速主力', icon: 'fas fa-motorcycle' },
  { value: 'BIKE', label: '自行车', note: '短途低碳', icon: 'fas fa-bicycle' },
  { value: 'WALK', label: '步行', note: '校园专送', icon: 'fas fa-walking' }
];

const load = async () => {
  try {
    const response = await request.get('/api/v1/riders/me');
    profile.value = response.data || null;
    if (profile.value?.auditStatus === 2) {
      form.realName = profile.value.realName || '';
      form.phone = profile.value.phone || '';
      form.vehicleType = profile.value.vehicleType || 'E_BIKE';
    }
  } catch (error) {
    toast.error(error.response?.data?.message || '读取骑手档案失败');
  } finally {
    loading.value = false;
  }
};

const submit = async () => {
  if (!/^1[3-9]\d{9}$/.test(form.phone)) return toast.warning('请填写正确的手机号');
  submitting.value = true;
  try {
    const response = await request.post('/api/v1/riders/applications', form);
    if (!response.success) throw new Error(response.message);
    profile.value = response.data;
    toast.success('申请已提交，请等待审核');
  } catch (error) {
    toast.error(error.response?.data?.message || error.message || '提交失败');
  } finally {
    submitting.value = false;
  }
};

const vehicleName = value => ({ E_BIKE: '电动车', BIKE: '自行车', WALK: '步行' }[value] || value);
const maskPhone = value => value?.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
onMounted(load);
</script>

<style scoped>
* { box-sizing: border-box; }
.apply-page { min-height: 100vh; background: #f4f7fb; color: #14213d; display: grid; grid-template-columns: minmax(320px, .9fr) minmax(440px, 1.1fr); }
.hero { position: relative; padding: clamp(40px, 7vw, 100px); color: #fff; background: radial-gradient(circle at 15% 12%, rgba(75,223,183,.35), transparent 27%), linear-gradient(150deg,#073b4c 0%,#0b6e75 55%,#14a887 100%); display: flex; flex-direction: column; justify-content: center; overflow: hidden; }
.hero::after { content: ''; position: absolute; width: 360px; height: 360px; right: -150px; bottom: -140px; border: 70px solid rgba(255,255,255,.08); border-radius: 50%; }
.back { position: absolute; top: 28px; left: 34px; color: rgba(255,255,255,.82); border: 0; background: transparent; cursor: pointer; font-size: 14px; }
.hero-icon { width: 76px; height: 76px; border-radius: 24px; display: grid; place-items: center; background: rgba(255,255,255,.14); backdrop-filter: blur(10px); font-size: 34px; margin-bottom: 28px; }
.eyebrow,.step { font-size: 12px; letter-spacing: 2px; font-weight: 800; color: #69e5c5; }
h1 { font-size: clamp(36px,4vw,62px); line-height: 1.12; max-width: 620px; margin: 12px 0 20px; }
.subtitle { max-width: 560px; color: rgba(255,255,255,.74); line-height: 1.8; }
.hero-points { margin-top: 46px; display: flex; flex-wrap: wrap; gap: 22px; color: rgba(255,255,255,.8); font-size: 13px; }
.hero-points b { display: block; color: #69e5c5; margin-bottom: 5px; font-size: 11px; }
.form-panel { display: grid; place-items: center; padding: 44px clamp(24px,6vw,90px); }
form,.state-card { width: min(100%, 560px); background: #fff; padding: clamp(28px,5vw,52px); border-radius: 28px; box-shadow: 0 24px 70px rgba(29,54,81,.1); }
.panel-heading { display: flex; justify-content: space-between; gap: 18px; align-items: flex-start; margin-bottom: 28px; }
.panel-heading h2,.state-card h2 { font-size: 26px; margin: 7px 0 0; }
label>span,legend { display: block; color: #405269; font-size: 13px; font-weight: 700; margin-bottom: 9px; }
label { display: block; margin-bottom: 20px; }
input:not([type=checkbox]) { width: 100%; height: 52px; border: 1px solid #dfe6ef; border-radius: 13px; padding: 0 16px; outline: 0; font-size: 15px; transition: .2s; }
input:focus { border-color: #13a98b; box-shadow: 0 0 0 4px rgba(19,169,139,.1); }
fieldset { border: 0; padding: 0; margin: 0 0 22px; }
.vehicle-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 10px; }
.vehicle-grid button { padding: 16px 8px; border: 1px solid #dfe6ef; background: #fff; border-radius: 15px; color: #405269; cursor: pointer; }
.vehicle-grid i,.vehicle-grid span,.vehicle-grid small { display: block; }
.vehicle-grid i { font-size: 21px; margin-bottom: 8px; }.vehicle-grid span{font-weight:800}.vehicle-grid small{color:#94a0ae;margin-top:4px}
.vehicle-grid button.selected { border-color: #13a98b; background: #effcf8; color: #07836d; box-shadow: inset 0 0 0 1px #13a98b; }
.agreement { display: flex; gap: 10px; align-items: flex-start; font-size: 12px; line-height: 1.6; color: #738094; }
.agreement input { margin-top: 3px; accent-color: #0da88a; }.agreement span{font-weight:400;margin:0}
.primary,.secondary { width: 100%; height: 54px; border: 0; border-radius: 14px; font-weight: 800; cursor: pointer; }
.primary { color: #fff; background: linear-gradient(135deg,#08a88a,#24c8a1); box-shadow: 0 12px 24px rgba(8,168,138,.24); }.primary:disabled{opacity:.45;cursor:not-allowed}
.secondary { margin-top: 22px; color: #087d6a; background: #eafaf6; }
.privacy { text-align: center; color: #9aa5b3; font-size: 11px; margin-top: 14px; }
.rejected,.status-pill { padding: 7px 11px; border-radius: 20px; background: #fff1f0; color: #d94841; font-size: 12px; font-weight: 800; }
.reject-note { padding: 12px 14px; border-radius: 11px; color: #b4443d; background: #fff5f3; font-size: 13px; margin: -12px 0 22px; }
.state-card { text-align: center; }.state-icon { width: 82px; height:82px; display:grid;place-items:center;border-radius:50%;margin:0 auto 20px;background:#e9fbf6;color:#0ca386;font-size:30px}
.state-card .status-pill { display:inline-block;background:#fff8df;color:#a67400}.state-card.approved .status-pill{background:#e9fbf6;color:#07836d}
.state-card p{color:#6d7a8c;line-height:1.8;margin:14px 0}.profile-preview{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:24px}.profile-preview span{padding:14px;background:#f7f9fc;border-radius:12px;color:#8894a3;font-size:12px}.profile-preview b{display:block;color:#27374c;margin-top:5px}
@media (max-width: 800px) { .apply-page{display:block}.hero{min-height:420px;padding:100px 28px 45px}.form-panel{padding:24px 16px 50px;margin-top:-24px;position:relative}.vehicle-grid{grid-template-columns:1fr}.back{left:24px}.hero-points{gap:16px} }

/* 骑手申请页回归项目主色，保留清晰的表单层次，不使用渐变和装饰光晕 */
.apply-page { display: block; max-width: 1100px; margin: 0 auto; background: #f5f9fd; color: #24405c; }
.hero { min-height: 0; padding: 28px 32px 24px; color: #24405c; background: #eaf5ff; border-bottom: 1px solid #d5e9f8; }
.hero::after { display: none; }
.back { position: static; margin-bottom: 22px; padding: 0; color: #2878ad; font-size: 13px; }
.hero-icon { width: 56px; height: 56px; margin-bottom: 18px; border-radius: 10px; background: #fff; color: #0097ff; border: 1px solid #cfe6f8; backdrop-filter: none; font-size: 25px; }
.eyebrow, .step { color: #4b86ae; letter-spacing: 1px; }
.hero h1 { max-width: 620px; margin: 8px 0 10px; color: #173b60; font-size: clamp(28px, 4vw, 46px); }
.subtitle { max-width: 560px; color: #637f97; line-height: 1.65; }
.hero-points { margin-top: 24px; gap: 18px; color: #637f97; }
.hero-points b { color: #0879c7; }
.form-panel { display: grid; place-items: center; padding: 24px 16px 72px; }
form, .state-card { width: min(100%, 560px); padding: 28px; border: 1px solid #dfeaf5; border-radius: 12px; box-shadow: 0 4px 18px rgba(36,91,132,.08); }
.panel-heading { margin-bottom: 22px; }
.panel-heading h2, .state-card h2 { color: #24405c; font-size: 22px; }
label > span, legend { color: #405f79; }
input:not([type=checkbox]) { height: 46px; border-color: #cfdfeb; border-radius: 6px; }
input:focus { border-color: #0097ff; box-shadow: 0 0 0 3px rgba(0,151,255,.12); }
.vehicle-grid button { padding: 13px 8px; border-color: #cfdfeb; border-radius: 7px; color: #405f79; }
.vehicle-grid button.selected { border-color: #0097ff; background: #edf7ff; color: #0879c7; box-shadow: inset 0 0 0 1px #0097ff; }
.agreement { color: #71879a; }
.agreement input { accent-color: #0097ff; }
.primary { color: #fff; background: #0097ff; box-shadow: none; border-radius: 7px; }
.primary:hover:not(:disabled) { background: #087dcc; }
.secondary { margin-top: 14px; color: #0879c7; background: #edf7ff; border-radius: 7px; }
.privacy { color: #8498aa; }
.rejected, .status-pill { border-radius: 6px; }
.state-icon { background: #eaf5ff; color: #0879c7; }
.state-card .status-pill { background: #edf7ff; color: #0879c7; }
.state-card.approved .status-pill { background: #eaf7ef; color: #2a7a4b; }
.profile-preview span { background: #f5f9fd; border: 1px solid #e1edf7; }
.profile-preview b { color: #24405c; }
@media (max-width: 800px) {
  .hero { padding: 24px 20px 22px; }
  .hero h1 { font-size: 30px; }
  .form-panel { padding: 16px 12px 72px; margin-top: 0; }
  form, .state-card { padding: 22px 18px; border-radius: 10px; }
  .vehicle-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 420px) {
  .vehicle-grid { grid-template-columns: 1fr; }
}
</style>
