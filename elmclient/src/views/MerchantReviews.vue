<template>
  <div class="reviews-page"><header><button @click="$router.back()">‹</button><h1>顾客评价</h1></header><main>
    <div v-if="loading" class="empty">加载中...</div><div v-else-if="!reviews.length" class="empty">暂时还没有顾客评价</div>
    <article v-for="review in reviews" :key="review.id" class="review-card"><div class="review-head"><strong>{{ review.customerName || '匿名顾客' }}</strong><span class="stars">{{ '★'.repeat(review.rating) }}{{ '☆'.repeat(5-review.rating) }}</span><time>{{ formatDate(review.createTime) }}</time></div><p>{{ review.content || '用户未填写文字评价' }}</p><div v-if="review.merchantReply" class="reply">已回复：{{ review.merchantReply }}</div><div v-else class="reply-form"><input v-model="replyDraft[review.id]" maxlength="500" placeholder="回复这条评价"><button @click="reply(review)">回复</button></div></article>
  </main></div>
</template>
<script setup>
import { ref, onMounted } from 'vue'; import request from '../utils/request'; import { toast } from '../utils/toast';
const reviews=ref([]),loading=ref(true),replyDraft=ref({}),businessId=ref(null);
const load=async()=>{try{const stores=await request.get('/api/businesses/id_list'); const first=stores?.data?.[0]; businessId.value=first?.merchantId || first?.businessId || first?.id; if(businessId.value){const res=await request.get(`/api/v1/reviews/business/${businessId.value}`); if(res.success) reviews.value=res.data||[]}}finally{loading.value=false}};
const reply=async(review)=>{const text=replyDraft.value[review.id]; if(!text?.trim()) return toast.warning('请输入回复内容'); const res=await request.put(`/api/v1/reviews/${review.id}/reply`,{reply:text}); if(res.success){review.merchantReply=text.trim();toast.success('回复已提交')}};
const formatDate=(v)=>v?new Date(v).toLocaleDateString('zh-CN'):''; onMounted(load);
</script>
<style scoped>
.reviews-page{min-height:100vh;background:#f5f8fc;color:#29445d}header{height:56px;background:#168bd1;color:#fff;display:flex;align-items:center;gap:12px;padding:0 16px}header button{border:0;background:none;color:#fff;font-size:30px}h1{font-size:18px;margin:0}main{max-width:680px;margin:auto;padding:16px}.empty{text-align:center;color:#8aa0b2;padding:60px 0}.review-card{background:#fff;border:1px solid #deebf4;border-radius:10px;padding:15px;margin-bottom:12px}.review-head{display:flex;align-items:center;gap:10px}.review-head strong{color:#385b75}.review-head time{margin-left:auto;color:#9aacb9;font-size:12px}.stars{color:#efb33a;letter-spacing:1px}.review-card p{color:#526d82;line-height:1.6;margin:12px 0}.reply{background:#f3f8fc;border-left:3px solid #168bd1;padding:8px 10px;color:#557186;font-size:13px}.reply-form{display:flex;gap:8px}.reply-form input{min-width:0;flex:1;border:1px solid #d6e4ef;border-radius:6px;padding:8px}.reply-form button{border:0;background:#168bd1;color:#fff;border-radius:6px;padding:0 14px}
</style>
