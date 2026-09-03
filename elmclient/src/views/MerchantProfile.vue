<template>
  <div class="container">
    <div class="top-background">
      <h1>商家信息</h1>
    </div>

    <div class="user-card">
      <div class="user-info-row">
        <div class="avatar">
          <img :src="merchant?.avatar || defaultAvatar" alt="商家头像" @error="handleImageError">
        </div>
        <div class="user-details">
          <div class="user-name">
            <i class="fas fa-user-circle user-icon"></i>
            <span>{{ merchant?.name || '未设置商家名称' }}</span>
          </div>
          <div class="user-phone">
            <i class="fas fa-phone phone-icon"></i>
            <span>{{ formattedPhone }}</span>
          </div>
        </div>
      </div>
      <div class="user-actions">
        <button class="switch-btn" @click="switchToCustomer">
          <i class="fas fa-user"></i> 切换为顾客
        </button>
        <button class="logout-btn" @click="logout">
          <i class="fas fa-sign-out-alt"></i> 退出登录
        </button>
      </div>
    </div>
    <section class="merchant-entry-card">
      <div class="entry-heading">
        <div>
          <span class="entry-kicker">经营管理</span>
          <h2>店铺与菜单</h2>
          <p>店铺资料、营业状态和商品统一在工作台维护。</p>
        </div>
        <button class="entry-button" @click="goToWorkbench">进入工作台 <i class="fas fa-arrow-right"></i></button>
      </div>
      <div class="entry-stats">
        <div><strong>{{ stores.length }}</strong><span>店铺总数</span></div>
        <div><strong>{{ approvedStoreCount }}</strong><span>营业中</span></div>
        <div><strong>{{ pendingStoreCount }}</strong><span>审核中</span></div>
      </div>
      <button class="reviews-link" @click="$router.push('/merchant/reviews')"><i class="fas fa-comment-alt"></i> 查看顾客评价并回复</button>
    </section>

    <section v-if="stores.length === 0 && !loading" class="empty-store-tip">
      <i class="fas fa-store"></i>
      <p>还没有店铺，去工作台申请第一家店吧。</p>
      <button @click="goToWorkbench">申请新店</button>
    </section>


  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { toast } from '../utils/toast'; 
import request from '../utils/request';

export default {
  name: 'MerchantProfile',
  setup() {
    const router = useRouter();
    const defaultAvatar = require('@/assets/default-avatar.png'); // 备用默认头像

    const merchant = ref(null);
    const stores = ref([]); // 仅保留门店数量与状态摘要，明细统一在工作台维护
    
    const loading = ref(false);
    const approvedStoreCount = computed(() => stores.value.filter(store => store.status === 1).length);
    const pendingStoreCount = computed(() => stores.value.filter(store => store.status === 0).length);

    // 格式化手机号显示
    const formattedPhone = computed(() => {
      if (!merchant.value?.phone) return '未绑定手机';
      return merchant.value.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
    });

    onMounted(async () => {
      await loadMerchantData();
      // 在获取到 merchant.id 后，再加载统计数据
      if (merchant.value?.id) {
        await loadMerchantStores(merchant.value.id);
      }
    });

    // 加载商家基本信息
    const loadMerchantData = async () => {
      loading.value = true;
      try {
        const data = await request.get('/api/person');
        
        if (data && data.id) {
          merchant.value = {
            id: data.id,
            name: data.username,
            phone: data.phone,
            avatar: data.photo,
          };
        } else {
          toast.error('获取商家信息失败：服务器返回数据为空或格式不正确！');
        }
      } catch (error) {
        console.error('获取商家信息失败:', error);
        toast.error('获取商家信息失败，请重试！');
      } finally {
        loading.value = false;
      }
    };
    
    // “我的”只显示门店摘要，门店明细统一由经营工作台负责
    const loadMerchantStores = async (userId) => {
      try {
        const response = await request.get(`/api/businesses/merchant?userId=${userId}`);
        
        if (response && response.success && response.data) {
          stores.value = Array.isArray(response.data) ? response.data : [];
        } else {
          stores.value = [];
        }
      } catch (error) {
        console.error('获取商家店铺摘要失败:', error);
        toast.error('获取店铺信息失败，请重试！');
      }
    };

    const logout = () => {
      sessionStorage.removeItem('merchant');
      sessionStorage.removeItem('user');
      // 清空所有本地存储，确保完全退出
      localStorage.clear();
      sessionStorage.clear();
      router.push({ path: '/index' });
    };

    const switchToCustomer = () => {
      router.push({ path: '/myInformation' });
    };
    const goToWorkbench = () => {
      router.push('/merchant/business');
    };
    const handleImageError = (event) => {
      const image = event?.target;
      if (!image || image.dataset.fallbackApplied === 'true') return;
      image.dataset.fallbackApplied = 'true';
      image.src = defaultAvatar;
    };

    return {
      merchant,
      stores,
      formattedPhone,
      loading,
      approvedStoreCount,
      pendingStoreCount,
      defaultAvatar,
      logout,
      switchToCustomer,
      goToWorkbench,
      handleImageError
    };
  }
};
</script>

<style scoped>
/* 原有样式保持不变 */
.container {
  max-width: 600px;
  margin: 0 auto;
  background: #fff;
  min-height: 100vh;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  padding-bottom: 40rpx;
}
/* ----------------------- 顶部标题栏 ----------------------- */
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
.user-card {
  width: 92%;
  max-width: 500px;
  margin: 120px auto 20px; /* 为固定头部留出空间 */
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: relative;
  z-index: 2;
}

.user-card .user-info-row {
  display: flex;
  align-items: center;
  gap: 20px;
}
.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid white;
  box-shadow: 0 6px 20px rgba(0, 151, 255, 0.3);
  flex-shrink: 0;
  background: #f8f9fa;
  margin-left: 15px;
}
.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}
.user-details {
  flex: 1;
  background-color: #f8f9fa;
  padding: 15px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid #e9ecef;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.user-actions {
  display: flex;
  gap: 15px;
  width: 100%;
}
.user-name, .user-phone {
  font-size: 0.95rem;
  color: #495057;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  padding: 10px;
  display: flex;
  align-items: center;
}
.user-name {
  font-size: 1.1rem;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}
.user-name .user-icon, 
.user-phone .phone-icon {
  margin-right: 8px;
  color: #3498db;
}

/* 新增商铺列表样式 - 房子造型 */
.stores-container {
  width: 92%;
  max-width: 500px;
  margin: 20px auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
  /* 添加 overflow: hidden 来隐藏超出的部分 */
  overflow: hidden;
}

.store-card {
  position: relative;
  background: #fff;
  border-radius: 16px 16px 16px 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  padding: 50px 15px 15px;
  margin-top: 10px;
  /* 改为 hidden 来隐藏超出的屋顶部分 */
  overflow: hidden;
}

/* 房子屋顶 - 在卡片内部显示 */
.store-card::before {
  content: '';
  position: absolute;
  top: -20px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 60px solid transparent;
  border-right: 60px solid transparent;
  border-bottom: 30px solid #ff6b6b;
  z-index: 1;
}

/* 房子主体装饰 - 在卡片内部的横梁 */
.store-card::after {
  content: '';
  position: absolute;
  top: -10px;
  left: 50%;
  transform: translateX(-50%);
  width: 100px;
  height: 20px;
  background: linear-gradient(135deg, #ff6b6b, #ee5a52);
  border-radius: 0 0 10px 10px;
  z-index: 2;
  box-shadow: 0 2px 8px rgba(255, 107, 107, 0.3);
}

/* 门的装饰 */
.store-card .store-name::before {
  content: '🏪';
  position: absolute;
  top: -20px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 18px;
  z-index: 3;
}

.store-name {
  font-size: 1.2rem;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 15px;
  text-align: center;
  padding-top: 8px;
  position: relative;
  border-bottom: 2px solid #ecf0f1;
  padding-bottom: 12px;
}

/* 店铺数据栏样式 - 窗户效果 */
.store-data-bar {
  display: flex;
  justify-content: space-around;
  width: 100%;
  padding: 20px 10px 15px;
  border-radius: 12px;
  background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  border: 2px solid #dee2e6;
  position: relative;
  margin-top: 10px;
}

/* 窗户框架装饰 */
.store-data-bar::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 33.33%;
  transform: translateY(-50%);
  width: 1px;
  height: 60%;
  background: #ced4da;
}

.store-data-bar::after {
  content: '';
  position: absolute;
  top: 50%;
  right: 33.33%;
  transform: translateY(-50%);
  width: 1px;
  height: 60%;
  background: #ced4da;
}

.data-item {
  text-align: center;
  flex: 1;
  position: relative;
}

.data-value {
  font-size: 22px;
  font-weight: bold;
  color: #2c3e50;
  margin-bottom: 6px;
  text-shadow: 0 1px 2px rgba(0,0,0,0.1);
}

.data-label {
  font-size: 13px;
  color: #6c757d;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* 特殊数据项颜色 */
.data-item:nth-child(1) .data-value {
  color: #e74c3c; /* 点赞 - 红色 */
}

.data-item:nth-child(2) .data-value {
  color: #f39c12; /* 收藏 - 橙色 */
}

.data-item:nth-child(3) .data-value {
  color: #27ae60; /* 评分 - 绿色 */
}

/* 房子阴影效果 */
.store-card {
  box-shadow:
    0 8px 24px rgba(0, 0, 0, 0.12),
    0 2px 8px rgba(255, 107, 107, 0.2);
  transition: all 0.3s ease;
}

.store-card:hover {
  transform: translateY(-2px);
  box-shadow:
    0 12px 32px rgba(0, 0, 0, 0.15),
    0 4px 12px rgba(255, 107, 107, 0.3);
}

.switch-btn {
  flex: 1;
  padding: 12px;
  text-align: center;
  background: linear-gradient(135deg, #ff9a9e, #fad0c4);
  color: white;
  font-weight: 600;
  border-radius: 12px;
  border: none;
  font-size: 0.9rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 10px;
}
.switch-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}
.logout-btn {
  flex: 1;
  padding: 12px;
  text-align: center;
  background: linear-gradient(135deg, #8e2de2, #4a00e0);
  color: white;
  font-weight: 600;
  border-radius: 12px;
  border: none;
  font-size: 0.9rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
}
.logout-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}
.switch-btn i,
.logout-btn i {
  margin-right: 6px;
}

/* 底部导航栏样式 */
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 600px;
  background-color: #fff;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: space-around;
  align-items: center;
  height: 8vh;
  border-radius: 16px 16px 0 0;
  z-index: 1000;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 12px;
  text-decoration: none;
  flex: 1;
  transition: color 0.3s;
}

.nav-item.active {
  color: #4CAF50;
}

.nav-item i {
  font-size: 20px;
  margin-bottom: 4px;
}

@media (max-width: 480px) {
  .container, .user-card {
    max-width: 100vw;
    width: 100vw;
    border-radius: 0;
  }

  .container {
    padding:0 0 80px 0;
  }

  .top-background {
    height: 90px;
    border-radius: 0;
    max-width: 100vw;
  }

  .user-card {
    padding: 20px 15px;
    margin-top: 110px;
    width: 90%;
    gap: 15px;
  }

  .user-info-row {
    flex-direction: column;
    align-items: center;
    gap: 15px;
  }

  .avatar {
    width: 80px;
    height: 80px;
  }

  .user-details {
    width: 100%;
    padding: 10px;
    gap: 6px;
  }

  .user-actions {
    flex-direction: column;
    gap: 10px;
  }

  .switch-btn,
  .logout-btn {
    width: 100%;
    padding: 14px;
    font-size: 0.95rem;
  }

  .bottom-nav {
    border-radius: 0;
  }
}

/* 商家“我的”只承担账号与入口职责，门店明细统一放在经营工作台。 */
.container {
  max-width: 600px;
  min-height: 100vh;
  margin: 0 auto;
  padding: 0 16px 88px;
  background: #f6f9fd;
  box-shadow: none;
  border-radius: 0;
  align-items: stretch;
}
.top-background {
  position: sticky;
  top: 0;
  left: auto;
  transform: none;
  width: calc(100% + 32px);
  max-width: none;
  height: 64px;
  margin-left: -16px;
  margin-bottom: 0;
  background: #fff;
  border-radius: 0;
  border-bottom: 1px solid #e3edf7;
  box-shadow: 0 2px 8px rgba(40, 92, 145, .05);
}
.top-background h1 {
  color: #173b62;
  font-size: 20px;
  text-shadow: none;
  letter-spacing: 0;
}
.user-card {
  width: 100%;
  max-width: none;
  margin: 18px 0 14px;
  padding: 18px;
  border: 1px solid #dfeaf5;
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(51, 101, 150, .05);
  transform: none;
  top: auto;
  gap: 16px;
}
.user-info-row { gap: 14px; }
.avatar { width: 64px; height: 64px; margin-left: 0; border: 2px solid #fff; box-shadow: 0 3px 10px rgba(0, 151, 255, .16); }
.user-details { padding: 0; margin-right: 0; background: transparent; border: 0; box-shadow: none; gap: 7px; }
.user-name, .user-phone { padding: 0; background: transparent; box-shadow: none; border-radius: 0; }
.user-name { color: #244d73; font-size: 17px; margin-bottom: 2px; }
.user-phone { color: #7189a2; font-size: 12px; }
.user-actions { gap: 10px; }
.switch-btn, .logout-btn { padding: 10px 12px; border-radius: 7px; border: 1px solid transparent; box-shadow: none; font-size: 12px; margin-bottom: 0; }
.switch-btn { background: #edf7ff; color: #1479c3; border-color: #cfe6f8; }
.logout-btn { background: #fff5f3; color: #c65a4d; border-color: #f2d8d3; }
.switch-btn:hover, .logout-btn:hover { transform: none; box-shadow: none; }
.merchant-entry-card {
  width: 100%;
  padding: 20px;
  background: #fff;
  border: 1px solid #dfeaf5;
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(51, 101, 150, .04);
}
.entry-heading { display: flex; align-items: center; justify-content: space-between; gap: 18px; }
.entry-kicker { color: #1683c8; font-size: 11px; font-weight: 700; }
.entry-heading h2 { margin: 6px 0; color: #244d73; font-size: 19px; }
.entry-heading p { color: #7c91a6; font-size: 12px; line-height: 1.6; }
.entry-button { flex: none; padding: 10px 13px; border: 0; border-radius: 7px; background: #0097ff; color: #fff; font-size: 12px; cursor: pointer; }
.entry-button i { margin-left: 5px; }
.entry-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-top: 18px; padding-top: 15px; border-top: 1px solid #edf3f8; }
.entry-stats div { text-align: center; }
.entry-stats strong, .entry-stats span { display: block; }
.entry-stats strong { color: #1e5a87; font-size: 20px; }
.entry-stats span { margin-top: 4px; color: #8398ad; font-size: 11px; }
.empty-store-tip { width: 100%; margin-top: 14px; padding: 28px 16px; text-align: center; background: #fff; border: 1px dashed #cfe1f1; border-radius: 12px; color: #8195a8; }
.empty-store-tip i { color: #6faeda; font-size: 24px; }
.empty-store-tip p { margin: 8px 0 13px; font-size: 12px; }
.empty-store-tip button { padding: 8px 16px; border: 1px solid #9dccf3; border-radius: 6px; background: #edf7ff; color: #1479c3; cursor: pointer; }
@media (max-width: 480px) {
  .container { padding-left: 12px; padding-right: 12px; }
  .top-background { width: calc(100% + 24px); margin-left: -12px; }
  .entry-heading { align-items: stretch; flex-direction: column; gap: 12px; }
  .entry-button { width: 100%; }
}
</style>

<style scoped>
.reviews-link{margin-top:12px;border:1px solid #168bd1;color:#168bd1;background:#fff;border-radius:7px;padding:9px 14px;cursor:pointer}.reviews-link:hover{background:#f2f8fd}
</style>
