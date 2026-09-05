<template>
  <main class="apply-page">
    <header><button type="button" aria-label="返回" @click="router.back()">‹</button><h1>申请成为商家</h1></header>
    <section class="apply-card">
      <div class="store-icon"><i class="fas fa-store"></i></div>
      <h2>开启你的店铺</h2>
      <p>提交身份申请后，管理员会进行审核。审核通过即可进入商家端，并继续填写店铺资料、申请开店。</p>
      <ol>
        <li><span>1</span><div><b>申请商家身份</b><small>当前这一步，只提交账号身份申请</small></div></li>
        <li><span>2</span><div><b>管理员审核</b><small>结果会出现在消息与通知中</small></div></li>
        <li><span>3</span><div><b>申请开店</b><small>补充店名、地址、配送和优惠信息</small></div></li>
      </ol>
      <button class="submit-button" type="button" :disabled="submitting || submitted" @click="submit">
        {{ submitting ? '正在提交…' : submitted ? '申请已提交' : '提交申请' }}
      </button>
      <button class="notice-link" type="button" @click="router.push('/notifications')">查看审核通知</button>
    </section>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';
import { toast } from '../utils/toast';
import { isAuthenticated } from '../utils/auth';

const router = useRouter();
const submitting = ref(false);
const submitted = ref(false);

onMounted(() => {
  if (!isAuthenticated()) router.replace({ path: '/login', query: { role: 'merchant', redirect: '/merchant/apply' } });
});

const submit = async () => {
  if (submitting.value || submitted.value) return;
  submitting.value = true;
  try {
    const response = await request.post('/api/permission/apply-merchant');
    if (!response?.success) throw new Error(response?.message || '提交失败');
    submitted.value = true;
    toast.success('申请已提交，请等待管理员审核');
  } catch (error) {
    const message = error?.response?.data?.message || error?.message || '提交失败，请稍后重试';
    if (message.includes('已提交')) submitted.value = true;
    toast.warning(message);
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
*{box-sizing:border-box}.apply-page{min-height:100vh;background:#f5f8fc;color:#29445d}.apply-page header{height:56px;background:#168bd1;color:#fff;display:flex;align-items:center;padding:0 16px;position:sticky;top:0}.apply-page header button{border:0;background:none;color:#fff;font-size:32px;line-height:1;padding:0 12px 0 0;cursor:pointer}.apply-page h1{font-size:18px;margin:0}.apply-card{width:min(calc(100% - 32px),560px);margin:28px auto;background:#fff;border:1px solid #dfebf4;border-radius:12px;padding:30px 24px;box-shadow:0 5px 18px rgba(38,89,128,.07);text-align:center}.store-icon{width:58px;height:58px;margin:0 auto 16px;background:#e9f5fd;color:#168bd1;border-radius:50%;display:grid;place-items:center;font-size:24px}.apply-card h2{margin:0 0 10px;font-size:23px;color:#244b68}.apply-card>p{margin:0 auto;color:#748b9d;font-size:14px;line-height:1.75;max-width:420px}.apply-card ol{list-style:none;padding:0;margin:28px 0;text-align:left;border-top:1px solid #e9f0f5}.apply-card li{display:flex;align-items:center;gap:13px;padding:15px 4px;border-bottom:1px solid #e9f0f5}.apply-card li>span{width:28px;height:28px;border-radius:50%;background:#eaf6fd;color:#168bd1;display:grid;place-items:center;font-weight:700}.apply-card li div{display:flex;flex-direction:column;gap:4px}.apply-card li b{font-size:14px}.apply-card li small{color:#8ba0b0;font-size:12px}.submit-button{width:100%;height:44px;border:0;border-radius:7px;background:#168bd1;color:#fff;font-weight:600;font-size:15px;cursor:pointer}.submit-button:disabled{background:#a9bfce;cursor:not-allowed}.notice-link{margin-top:12px;border:0;background:transparent;color:#168bd1;padding:8px;cursor:pointer}
</style>
