<template>
  <div class="favorites-page">
    <div class="header">
      <button class="back-button" type="button" aria-label="返回" @click="goBack">‹</button>
      <h1>我的收藏</h1>
    </div>
    <div class="container">
      <div v-if="loading" class="loading-message">
        <i class="fas fa-spinner fa-spin"></i> 加载中...
      </div>

      <div v-else-if="errorMessage" class="error-message">
        <i class="fas fa-exclamation-circle"></i> {{ errorMessage }}
      </div>

      <div v-else-if="favoriteList.length === 0" class="empty-message">
        <i class="fas fa-heart-broken"></i>
        <p>还没有收藏的店铺，快去逛逛吧！</p>
      </div>

      <div v-else class="business-list">
        <div v-for="business in favoriteList" :key="business.businessId" class="business-item"
          @click="goToBusinessInfo(business.businessId)">
          <img :src="business.businessImg || require('@/assets/business-default.png')"
     :alt="business.businessName"
     class="business-img"
     @error="handleImageError"><div class="business-details">
            <div class="business-name">{{ business.businessName }} <span class="status-tag" :class="{ closed: business.operatingStatus === false }">{{ business.operatingStatus === false ? '休息中' : '营业中' }}</span></div>
            <div class="business-info-row">
              <span class="star-rating" :class="{ unrated: business.starRating === null }">
                <i class="fas fa-star"></i> {{ business.starRating === null ? '暂无评分' : `${business.starRating}分` }}
              </span>
              <span class="sales-volume">月售{{ business.salesVolume }}单</span>
            </div>
            <div class="business-delivery-row">
              <span class="delivery-fee">配送费￥{{ business.deliveryFee.toFixed(2) }}</span>
              <span class="start-price">起送费￥{{ business.startPrice.toFixed(2) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { toast } from '@/utils/toast';
import request from '@/utils/request';
import { getStoredUser } from '@/utils/auth';
import { formatRating } from '@/utils/formatters';
export default {
  name: 'Favorites',
  setup() {
    const router = useRouter();
    const favoriteList = ref([]);
    const loading = ref(false);
    const errorMessage = ref('');

    const fetchFavorites = async () => {
      loading.value = true;
      errorMessage.value = '';
      try {
        const userInfo = getStoredUser();
        if (!userInfo || !userInfo.id) {
          toast.warning('请先登录以查看收藏列表！');
          router.replace({ path: '/login', query: { role: 'user', redirect: '/favorites' } });
          return;
        }

        // 调用后端API获取收藏列表
        const response = await request.get(`/api/merchant/interaction/collections/${userInfo.id}`);

        // 将后端返回的数据映射到前端需要的格式
        favoriteList.value = response.data.map(business => ({
          businessId: business.id,
          businessName: business.businessName,
          businessImg: business.businessImg,
          starRating: formatRating(business.score),
          salesVolume: business.salesCount || 0,
          deliveryFee: Number(business.deliveryPrice) || 0,
          startPrice: Number(business.startPrice) || 0,
          operatingStatus: business.operatingStatus
        }));

      } catch (error) {
        console.error('获取收藏列表失败:', error);
        errorMessage.value = '获取收藏列表失败，请稍后重试。';
        toast.error(errorMessage.value);
      } finally {
        loading.value = false;
      }
    };

    const goToBusinessInfo = (businessId) => {
      router.push({
        path: '/businessInfo',
        query: { businessId: businessId }
      });
    };

    const goBack = () => {
      router.back();
    };

    const handleImageError = (event) => {
      const image = event?.target;
      if (!image || image.dataset.fallbackApplied === 'true') return;
      image.dataset.fallbackApplied = 'true';
      image.src = require('@/assets/business-default.png');
    };

    onMounted(() => {
      fetchFavorites();
    });

    return {
      favoriteList,
      loading,
      errorMessage,
      goToBusinessInfo,
      goBack,
      handleImageError
    };
  }
};
</script>

<style scoped>
/* 整个页面的基本样式，确保没有默认外边距 */
body {
  margin: 0;
  padding: 0;
}

/* 顶部栏，不受 container 限制，贴近边缘 */
.header {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 50px;
  background-color: #0097ff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  z-index: 100;
}

/* 主内容容器，保持居中和最大宽度 */
.container {
  /* max-width: 600px; */
  width:100%;
  /* margin: 0 auto; */
  padding-bottom: 40px;
  background-color: #f0f2f5;
  min-height: 100vh;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  margin-top: calc(-13vw);
}

.header h1 {
  font-size: 1.1rem;
  color: #ffffff;
  font-weight: 600;
  color:white;
}

.back-icon {
  position: absolute;
  left: 15px; /* 调整左边距以更好地对齐 */
  font-size: 1.2rem;
  color: #ffffff;
  cursor: pointer;
  padding: 5px;
}

.favorites-page{min-height:100vh;background:#f4f8fb;color:#29445d}.favorites-page .header{left:50%;transform:translateX(-50%);max-width:600px}.favorites-page .container{width:100%;max-width:600px;margin:50px auto 0!important;padding:14px 0 40px;box-sizing:border-box;background:#f4f8fb}.back-button{position:absolute;left:12px;border:0;background:transparent;color:#fff;font-size:30px;line-height:1;cursor:pointer}.status-tag{display:inline-block;margin-left:6px;padding:2px 6px;border-radius:8px;background:#eaf7ef;color:#3a9660;font-size:10px;font-weight:500;vertical-align:2px}.status-tag.closed{background:#edf1f4;color:#8394a1}@media(max-width:600px){.favorites-page .header{left:0;transform:none}}

.loading-message,
.error-message,
.empty-message {
  text-align: center;
  padding: 50px 20px;
  color: #777;
  font-size: 1rem;
}

.error-message {
  color: #e74c3c;
}

.empty-message p {
  margin-top: 10px;
  font-size: 0.9rem;
  color: #999;
}

.empty-message .fas {
  font-size: 2.5rem;
  color: #ccc;
  margin-bottom: 10px;
}

.loading-message .fas {
  color: #3498db;
  margin-right: 8px;
}

.business-list {
  padding: 0px 15px; /* 增加左右内边距，使列表内容与页面两边有一定间隔 */
  margin-top: 15px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.business-item {
  display: flex;
  align-items: center;
  background-color: #fff;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.business-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.business-img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  margin-right: 15px;
  border: 1px solid #eee;
}

.business-details {
  flex: 1;
}

.business-name {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.business-info-row,
.business-delivery-row {
  display: flex;
  align-items: center;
  font-size: 0.85rem;
  color: #777;
}

.business-info-row {
  margin-bottom: 5px;
}

.star-rating i {
  color: #f39c12;
  margin-right: 5px;
}

.star-rating.unrated,
.star-rating.unrated i {
  color: #8fa1ad;
}

.sales-volume {
  margin-left: 15px;
}

.delivery-fee {
  margin-right: 15px;
}

.start-price {
  color: #ff5722;
  font-weight: 500;
}
</style>
