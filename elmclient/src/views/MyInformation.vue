<template>
  <div class="container">
    <!-- 固定顶部栏 -->
    <div class="fixed-top">
      <div class="top-background">
        <h1>{{ riderMode ? '骑手中心' : '个人信息' }}</h1>
      </div>
    </div>

    <div class="user-card">
      <div class="avatar" @click="triggerFileInput">
        <img :src="userInfo?.photo || defaultUserAvatar" alt="用户头像" @error="handleImageError">
        <div class="avatar-overlay">
          <i class="fas fa-camera"></i>
          <span>更换头像</span>
        </div>
      </div>
      <div class="user-details">
        <div class="user-name">
          {{ userInfo?.username || '未设置昵称' }}
          <i class="fas fa-pencil-alt edit-icon" @click="openEditModal"></i>
        </div>
        <div class="user-full-name">
          <i class="fas fa-id-card-alt full-name-icon"></i>
          <span class="last-name">{{ userInfo?.lastName || '未设置姓氏' }}</span>
          <span class="first-name">{{ userInfo?.firstName || '未设置名字' }}</span>
        </div>
        <div class="user-phone">
          <i class="fas fa-phone phone-icon"></i>
          <span>{{ userInfo?.phone || '未设置手机号' }}</span>
        </div>
        <div class="user-email">
          <i class="fas fa-envelope-open-text email-icon"></i>
          <span>{{ userInfo?.email || '未设置邮箱' }}</span>
        </div>
      </div>
      <div class="card-button-section">
        <button v-if="riderMode" class="switch-btn rider-entry-btn" @click="backToRiderDashboard">
          <i class="fas fa-route"></i> 返回配送工作台
        </button>
        <button class="logout-btn" @click="logout">
          <i class="fas fa-sign-out-alt"></i>退出登录
        </button>
      </div>
    </div>

    <!-- 隐藏的文件输入框 -->
    <input type="file" ref="fileInput" style="display: none" accept="image/*" @change="handleFileUpload">

    <div v-if="!riderMode" class="menu-section">
      <div class="section-title">常用功能</div>
      <div class="menu-list">
        <div class="menu-item" @click="showAddressSection = !showAddressSection">
          <div class="menu-icon">
            <i class="fas fa-map-marker-alt"></i>
          </div>
          <span class="menu-text">收货地址</span>
          <i class="fas fa-chevron-right menu-arrow"></i>
        </div>
        <AddressManager v-if="showAddressSection" :id="userInfo?.id" class="address-manager" />
        <div class="menu-item" @click="myfavorite">
          <div class="menu-icon">
            <i class="fas fa-heart"></i>
          </div>
          <span class="menu-text">我的收藏</span>
          <i class="fas fa-chevron-right menu-arrow"></i>
        </div>
        <div class="menu-item message-item" @click="navigateTo('notifications')">
          <div class="menu-icon">
            <i class="fas fa-bell"></i>
          </div>
          <span class="menu-text">消息与通知</span>
          <div class="notification-badge" v-if="unreadMessageCount > 0">
            {{ unreadMessageCount }}
          </div>
          <i class="fas fa-chevron-right menu-arrow"></i>
        </div>
        <div class="menu-item" @click="navigateTo('assets')">
          <div class="menu-icon"><i class="fas fa-wallet"></i></div>
          <span class="menu-text">钱包与优惠</span>
          <i class="fas fa-chevron-right menu-arrow"></i>
        </div>
        <div class="menu-item" @click="navigateTo('preferences')">
          <div class="menu-icon"><i class="fas fa-palette"></i></div>
          <span class="menu-text">偏好与外观</span>
          <i class="fas fa-chevron-right menu-arrow"></i>
        </div>
      </div>
    </div>

    <div v-else class="menu-section rider-menu-section">
      <div class="section-title">骑手工具</div>
      <div class="menu-list">
        <div class="menu-item" @click="goRiderTab('active')">
          <div class="menu-icon"><i class="fas fa-route"></i></div>
          <span class="menu-text">配送中的订单</span>
          <i class="fas fa-chevron-right menu-arrow"></i>
        </div>
        <div class="menu-item" @click="goRiderTab('history')">
          <div class="menu-icon"><i class="fas fa-history"></i></div>
          <span class="menu-text">历史配送</span>
          <i class="fas fa-chevron-right menu-arrow"></i>
        </div>
        <div class="menu-item message-item" @click="navigateTo('notifications')">
          <div class="menu-icon"><i class="fas fa-bell"></i></div>
          <span class="menu-text">消息与通知</span>
          <div class="notification-badge" v-if="unreadMessageCount > 0">{{ unreadMessageCount }}</div>
          <i class="fas fa-chevron-right menu-arrow"></i>
        </div>
        <div class="menu-item" @click="openEditModal">
          <div class="menu-icon"><i class="fas fa-id-card"></i></div>
          <span class="menu-text">个人资料</span>
          <i class="fas fa-chevron-right menu-arrow"></i>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="uploading" class="upload-loading">
      <i class="fas fa-spinner fa-spin"></i> 上传中...
    </div>

    <div class="loading" v-if="loading">
      <i class="fas fa-spinner fa-spin"></i> 加载中...
    </div>

    <div class="error-message" v-if="errorMessage">
      <i class="fas fa-exclamation-circle"></i> {{ errorMessage }}
    </div>

    <div v-if="showEditModal" class="modal-overlay">
      <div class="modal-content">
        <h3>编辑个人信息</h3>
        <div class="modal-item">
          <label>姓氏</label>
          <input v-model="editFormData.lastName" placeholder="输入姓氏" />
        </div>
        <div class="modal-item">
          <label>名字</label>
          <input v-model="editFormData.firstName" placeholder="输入名字" />
        </div>
        <div class="modal-item">
          <label>手机号</label>
          <input v-model="editFormData.phone" placeholder="输入手机号" />
        </div>
        <div class="modal-item">
          <label>邮箱</label>
          <input v-model="editFormData.email" placeholder="输入邮箱" type="email" />
        </div>
        <div class="modal-buttons">
          <button @click="submitEdits">提交</button>
          <button @click="closeEditModal">取消</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script>
// (保持 script 部分不变)
import { ref, computed, onMounted, onUnmounted } from 'vue';
import AddressManager from '../components/AddressManager.vue';
import request from '../utils/request';
import { useRoute, useRouter } from 'vue-router';
import { toast } from '../utils/toast';
import { createRealtimeConnection } from '../services/realtimeService';
import { hasAuthority } from '../utils/roles';
import { clearAuth, getToken, updateStoredUser } from '../utils/auth';
import { DEFAULT_USER_AVATAR } from '../utils/profileDefaults';

export default {
  name: 'MyApplication',
  components: {
    AddressManager
  },
  setup() {
    const route = useRoute();
    const router = useRouter();
    const userInfo = ref({});
    const defaultUserAvatar = DEFAULT_USER_AVATAR;
    const loading = ref(false);
    const errorMessage = ref('');
    const showEditModal = ref(false);
    const showAddressSection = ref(false);
    const unreadMessageCount = ref(0);
    const uploading = ref(false);
    const fileInput = ref(null);
    let realtimeConnection = null;
    const riderMode = computed(() => route.query.role === 'rider');

    const editFormData = ref({
      firstName: '',
      lastName: '',
      phone: '',
      email: ''
    });

    const formattedPhone = computed(() => {
      if (!userInfo.value.phone) return '未绑定手机';
      return userInfo.value.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
    });

    onMounted(async () => {
      const token = getToken();
      if (!token) {
        toast.warning('用户未登录，请先登录！');
        router.push({ path: '/login', query: { role: riderMode.value ? 'rider' : 'user' } });
        return;
      }
      await loadUserData();
      await checkNewMessages();
      realtimeConnection = createRealtimeConnection({
        onMessage: handleNewMessage,
        onFallbackRefresh: checkNewMessages
      });
      realtimeConnection.start();
    });
    onUnmounted(() => {
      realtimeConnection?.stop();
    });
    const handleNewMessage = (message) => {
      const content = message.notificationContent || message.content || '您有一条新消息';
      toast.info(`新消息：${content}`);
      if (content.includes('您的成为商家申请已通过审核')) {
        if (userInfo.value.authorities && Array.isArray(userInfo.value.authorities)) {
          const hasBusinessAuth = hasAuthority(userInfo.value, 'BUSINESS');
          if (!hasBusinessAuth) {
            userInfo.value.authorities.push({ name: 'BUSINESS' });
            updateStoredUser(userInfo.value);
          }
        }
      }
      setTimeout(() => {
        checkNewMessages().catch(err => {
          console.error('新消息触发重新检查失败:', err);
          toast.error('新消息已收到，但加载失败');
        });
      }, 300);
    };
    const checkNewMessages = async () => {
      try {
        const token = getToken();
        if (!token) return;
        const response = await request.get('/api/notifications', {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });
        if (response && response.success && response.data) {
          unreadMessageCount.value = response.data.filter(
            item => item.isDeleted === 0 && item.isRead === 0
          ).length;
        } else {
          unreadMessageCount.value = 0;
        }
      } catch (error) {
        console.error('检查未读消息失败:', error);
        unreadMessageCount.value = 0;
      }
    };
    const triggerFileInput = () => {
      fileInput.value.click();
    };
    const handleFileUpload = async (event) => {
      const file = event.target.files[0];
      if (!file) return;
      if (!file.type.startsWith('image/')) {
        toast.error('请选择图片文件！');
        return;
      }
      if (file.size > 5 * 1024 * 1024) {
        toast.error('图片大小不能超过5MB！');
        return;
      }
      uploading.value = true;
      try {
        const token = getToken();
        const formData = new FormData();
        formData.append('file', file);
        const uploadResponse = await request.post('/upload', formData, {
          headers: {
            'Authorization': `Bearer ${token}`,
          }
        });
        if (uploadResponse && uploadResponse.data) {
          const updateResponse = await request.put('/api/person/info', {
            id: userInfo.value.id,
            photo: uploadResponse.data
          }, {
            headers: {
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'application/json'
            }
          });
          if (updateResponse && updateResponse.success) {
            userInfo.value.photo = uploadResponse.data;
            updateStoredUser(userInfo.value);
            toast.success('头像更新成功！');
          } else {
            toast.error('头像更新失败！');
          }
        } else {
          toast.error('图片上传失败！');
        }
      } catch (error) {
        console.error('头像上传失败:', error);
        toast.error('头像上传失败，请重试！');
      } finally {
        uploading.value = false;
        event.target.value = '';
      }
    };
    const loadUserData = async () => {
      loading.value = true;
      errorMessage.value = '';
      try {
        const token = getToken();
        if (!token) {
          toast.warning('用户未登录，请先登录！');
          router.push({ path: '/login', query: { role: riderMode.value ? 'rider' : 'user' } });
          return;
        }
        const response = await request.get('/api/person', {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });
        if (response) {
          userInfo.value = response;
          updateStoredUser(userInfo.value);
          console.log('用户信息加载成功:', userInfo.value);
        }
      } catch (error) {
        console.error('获取用户信息失败:', error);
        if (error.response && error.response.status === 401) {
          toast.error('登录已过期，请重新登录！');
          clearAuth();
          router.push({ path: '/login', query: { role: riderMode.value ? 'rider' : 'user' } });
        } else {
          errorMessage.value = '获取用户信息失败，请重试！';
          toast.error('获取用户信息失败，请重试！');
        }
      } finally {
        loading.value = false;
      }
    };
    const logout = () => {
      clearAuth();
      router.push({ path: '/index' });
    };
    const backToRiderDashboard = () => {
      router.push({ path: '/rider/dashboard', query: { tab: 'available' } });
    };
    const goRiderTab = (tab) => {
      router.push({ path: '/rider/dashboard', query: { tab } });
    };
    const openEditModal = () => {
      if (userInfo.value) {
        editFormData.value.firstName = userInfo.value.firstName || '';
        editFormData.value.lastName = userInfo.value.lastName || '';
        editFormData.value.phone = userInfo.value.phone || '';
        editFormData.value.email = userInfo.value.email || '';
      }
      showEditModal.value = true;
    };
    const closeEditModal = () => {
      showEditModal.value = false;
    };
    const submitEdits = async () => {
      if (!editFormData.value.phone) {
        toast.warning('手机号不能为空！');
        return;
      }
      try {
        const token = getToken();
        const response = await request.put('/api/person/info', {
          id: userInfo.value.id,
          firstName: editFormData.value.firstName,
          lastName: editFormData.value.lastName,
          email: editFormData.value.email,
          phone: editFormData.value.phone,
          photo: userInfo.value.photo
        }, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });
        if (response.success) {
          userInfo.value.firstName = editFormData.value.firstName;
          userInfo.value.lastName = editFormData.value.lastName;
          userInfo.value.email = editFormData.value.email;
          userInfo.value.phone = editFormData.value.phone;
          updateStoredUser(userInfo.value);
          toast.success('个人信息修改成功！');
          closeEditModal();
        } else {
          toast.error('个人信息修改失败！');
        }
      } catch (error) {
        console.error(error);
        toast.error('个人信息修改失败！');
      }
    };
    const myfavorite = () => {
      router.push({ path: '/favorites' });
    };
    const handleImageError = (event) => {
      const image = event?.target;
      if (!image || image.dataset.fallbackApplied === 'true') return;
      image.dataset.fallbackApplied = 'true';
      image.src = defaultUserAvatar;
    };
    const navigateTo = (page) => {
      const pageRoutes = {
        'notifications': '/notifications',
        'assets': '/assets',
        'preferences': '/preferences'
      };
      if (pageRoutes[page]) {
        router.push({
          path: pageRoutes[page],
          ...(riderMode.value && page === 'notifications' ? { query: { role: 'rider' } } : {})
        });
        if (page === 'notifications') {
          unreadMessageCount.value = 0;
        }
      } else {
        toast.warning('功能待开发');
      }
    };
    return {
      userInfo,
      defaultUserAvatar,
      formattedPhone,
      loading,
      uploading,
      errorMessage,
      showEditModal,
      editFormData,
      fileInput,
      logout,
      openEditModal,
      closeEditModal,
      submitEdits,
      myfavorite,
      handleImageError,
      navigateTo,
      backToRiderDashboard,
      goRiderTab,
      riderMode,
      showAddressSection,
      triggerFileInput,
      handleFileUpload,
      unreadMessageCount
    };
  },
};
</script>

<style scoped>
/* 保持原有的样式，只修改或新增以下部分 */
.container {
  width: 100%;
  max-width: 600px;
  background: #fff;
  min-height: 100vh;
  margin: 0 auto;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-radius: 0;
  padding-bottom: calc(96px + env(safe-area-inset-bottom));
  display: flex;
  flex-direction: column;
  align-items: stretch;
  position: relative;
  box-sizing: border-box;
}

/****************** 固定顶部栏 ******************/
.fixed-top {
  position: sticky;
  top: 0;
  left: auto;
  width: 100%;
  z-index: 1000;
  max-width: none;
  margin: 0;
}

.top-background {
  width: 100%;
  height: 72px;
  background: #168bd1;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 2px 8px rgba(25, 104, 156, 0.14);
  border-radius: 0;
  position: relative;
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

/****************** 内容区域 ******************/
.content-area {
  margin-top: 100px;
  /* 固定顶部栏的高度 */
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.user-card {
  width: calc(100% - 32px);
  max-width: 560px;
  margin: 18px auto 0;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(31, 75, 122, 0.10);
  padding: 20px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  z-index: 2;
  flex-wrap: wrap;
  justify-content: flex-start;
}

/* 新增：卡片内的按钮区域 */
.card-button-section {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 0;
  /* 增加内边距，保持与卡片一致 */
  box-sizing: border-box;
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
  margin-right: 15px;
}

.user-name,
.user-full-name,
.user-phone,
.user-email {
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

.user-full-name .first-name {
  margin-right: 5px;
}

.user-name .edit-icon,
.user-full-name .full-name-icon,
.user-phone .phone-icon,
.user-email .email-icon {
  margin-right: 8px;
  color: #3498db;
}

.edit-icon {
  margin-left: auto;
  color: #3498db;
  font-size: 16px;
  cursor: pointer;
}

.menu-section {
  width: calc(100% - 32px);
  max-width: 560px;
  margin: 24px auto 0;
}

.section-title {
  font-size: 1.1rem;
  color: #2c3e50;
  margin-bottom: 15px;
  padding-left: 10px;
  font-weight: 600;
  border-left: 4px solid #3498db;
}

.menu-list {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
}

.menu-item {
  display:flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.3s ease;
}

.menu-item:hover {
  background-color: #f1f8ff;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.08);
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-icon {
  width: 22px;
  height: 22px;
  margin-right: 15px;
  color: #3498db;
  display: flex;
  justify-content: center;
  align-items: center;
}

.menu-text {
  flex: 1;
  font-size: 0.95rem;
  color: #34495e;
  font-weight: 500;
}

.menu-arrow {
  color: #bdc3c7;
  font-size: 14px;
}

/* 移除 .button-section 样式，因为按钮已移动到 .user-card 中 */
.loading {
  text-align: center;
  padding: 15px;
  color: #3498db;
  font-size: 1rem;
}

.error-message {
  text-align: center;
  padding: 10px;
  background: #ffecec;
  color: #e74c3c;
  border-radius: 8px;
  margin: 10px;
  font-size: 0.9rem;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 20px;
  border-radius: 12px;
  max-width: 400px;
  width: 80%;
  box-sizing: border-box;
  text-align: center;
}

.modal-content h3 {
  margin-top: 0;
  color: #2c3e50;
  margin-bottom: 20px;
}

.modal-item {
  margin-bottom: 15px;
  text-align: left;
}

.modal-item label {
  display: block;
  font-weight: 500;
  color: #555;
  margin-bottom: 5px;
}

.modal-content input,
.modal-content textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 16px;
  box-sizing: border-box;
}

.modal-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.modal-buttons button {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
}

.modal-buttons button:first-child {
  background: #3498db;
  color: white;
  transition: background-color 0.3s;
}

.modal-buttons button:first-child:hover {
  background: #2980b9;
}

.modal-buttons button:last-child {
  background: #e0e0e0;
  color: #333;
  transition: background-color 0.3s;
}

.modal-buttons button:last-child:hover {
  background: #c7c7c7;
}

.menu-item.message-item {
  position: relative;
}

.rider-menu-section {
  margin-top: 80px;
}

.rider-menu-section .menu-item {
  min-height: 56px;
}

.notification-badge {
  position: absolute;
  top: 5px;
  right: 35px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  background-color: #ff4d4f;
  color: white;
  border-radius: 9px;
  font-size: 12px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

@media (max-width: 480px) {

  .container,
  .user-card,
  .menu-section,
  .card-button-section {
    max-width: 100vw;
  }

  .container {
    padding-bottom: calc(96px + env(safe-area-inset-bottom));
  }

  .top-background {
    height: 68px;
    border-radius: 0;
  }

  .user-card {
    flex-direction: column;
    align-items: center;
    gap: 10px;
    padding: 18px 14px;
    margin: 14px 12px 0;
    width: calc(100% - 24px);
    border-radius: 14px;
  }

  .avatar {
    width: 80px;
    height: 80px;
    margin-left: 0;
  }

  .user-details {
    width: 100%;
    padding: 10px;
    gap: 6px;
    margin-right: 0;
  }

  .menu-item {
    padding: 14px 16px;
  }

  .card-button-section {
    width: 100%;
    padding: 0;
    margin: 10px 0;
  }

  .menu-section {
    width: calc(100% - 24px);
  }
}

.avatar {
  position: relative;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.avatar:hover {
  transform: scale(1.05);
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s ease;
  border-radius: 50%;
}

.avatar:hover .avatar-overlay {
  opacity: 1;
}

.avatar-overlay i {
  font-size: 24px;
  margin-bottom: 5px;
}

.avatar-overlay span {
  font-size: 12px;
  text-align: center;
}

.upload-loading {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 20px;
  border-radius: 10px;
  z-index: 1000;
}

/* 修改按钮的渐变色 */
.switch-btn {
  width: 100%;
  padding: 14px;
  text-align: center;
  background: #168bd1;
  color: white;
  font-weight: 600;
  border-radius: 12px;
  border: none;
  font-size: 0.95rem;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 10px;
}

.switch-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

.rider-entry-btn {
  background: #159a78;
}

.logout-btn {
  width: 100%;
  padding: 14px;
  text-align: center;
  background: #4f83b8;
  color: white;
  font-weight: 600;
  border-radius: 12px;
  border: none;
  font-size: 0.95rem;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
}

.logout-btn:hover {
  background: #3d6f9f;
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.logout-btn i {
  margin-right: 8px;
}

.address-manager {
  margin-top: -120px; /* 向上移动 */
  transform: translateY(-20px); /* 进一步调整位置 */
}

/* 页面层级整理：固定顶部和底部导航不应遮挡内容。 */
.menu-section { margin-top: 24px; }
.rider-menu-section { margin-top: 24px; }
.address-manager { margin-top: 16px; transform: none; }
.menu-list > .address-manager {
  width: 100%;
  max-width: none;
  margin: 0;
  transform: none;
  border-top: 1px solid #eef3f7;
}
.menu-list > .address-manager .section-title {
  margin: 14px 16px 10px;
  font-size: 15px;
}
.menu-list > .address-manager .menu-list {
  border-radius: 0;
  box-shadow: none;
}
</style>
