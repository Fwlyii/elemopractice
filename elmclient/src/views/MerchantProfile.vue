<template>
  <div class="container">
    <div class="top-background">
      <h1>商家信息</h1>
    </div>

    <div class="user-card">
      <div class="user-info-row">
        <div class="avatar">
          <img :src="merchant?.avatar || defaultAvatar" alt="商家头像">
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
    <!-- 修改为显示商铺列表 -->
    <div class="stores-container">
      <div class="store-card" v-for="store in stores" :key="store.merchantId">
        <div class="store-name">{{ store.merchantName }}</div>
        <div class="store-data-bar">
          <div class="data-item">
            <div class="data-value">{{ store.likeCount }}</div>
            <div class="data-label">点赞</div>
          </div>
          <div class="data-item">
            <div class="data-value">{{ store.collectCount }}</div>
            <div class="data-label">收藏</div>
          </div>
          <div class="data-item">
            <div class="data-value">{{ store.rating }}</div>
            <div class="data-label">评分</div>
          </div>
        </div>
      </div>
    </div>

    

    <div class="bottom-nav">
      <router-link to="/merchant/business" class="nav-item">
        <i class="fas fa-store"></i>
        <span>商铺</span>
      </router-link>
      <router-link to="/merchant/orders" class="nav-item">
        <i class="fas fa-list-alt"></i>
        <span>订单</span>
      </router-link>
      <router-link to="/merchant/profile" class="nav-item active">
        <i class="fas fa-user-circle"></i>
        <span>我的</span>
      </router-link>
    </div>
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
    const stores = ref([]); // 存储商铺列表
    
    const loading = ref(false);

    // 格式化手机号显示
    const formattedPhone = computed(() => {
      if (!merchant.value?.phone) return '未绑定手机';
      return merchant.value.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
    });

    onMounted(async () => {
      await loadMerchantData();
      // 在获取到 merchant.id 后，再加载统计数据
      if (merchant.value?.id) {
        await loadMerchantStats(merchant.value.id);
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
    
    // 修改：加载商家统计数据，现在获取的是商铺列表
    const loadMerchantStats = async (userId) => {
      try {
        const response = await request.get(`http://110.42.60.144:8080/api/merchant/interaction/statsByUserId/${userId}`);
        
        if (response && response.success && response.data) {
          stores.value = response.data;
        } else {
          toast.error('获取商家统计数据失败！');
        }
      } catch (error) {
        console.error('获取商家统计数据失败:', error);
        toast.error('获取商家统计数据失败，请重试！');
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
      router.push({ path: '/myinformation' });
    };

    return {
      merchant,
      stores,
      formattedPhone,
      loading,
      defaultAvatar,
      logout,
      switchToCustomer
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
</style>