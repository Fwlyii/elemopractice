<template>
  <div class="manage-user-container">
    <div class="container">
      <div class="top-background">
        <h1>用户管理</h1>
      </div>

      <div class="search-section">
        <div class="search-box">
          <i class="fas fa-search"></i>
          <input 
            v-model="searchKeyword" 
            type="text" 
            placeholder="搜索用户名、手机号或邮箱"
            @input="handleSearch"
          />
        </div>
      </div>

      <div class="filter-section">
        <div class="filter-tabs">
          <div 
            class="filter-tab" 
            :class="{ active: activeFilter === 'all' }"
            @click="setFilter('all')"
          >
            全部用户
          </div>
          <div 
            class="filter-tab" 
            :class="{ active: activeFilter === 'enabled' }"
            @click="setFilter('enabled')"
          >
            已启用
          </div>
          <div 
            class="filter-tab" 
            :class="{ active: activeFilter === 'disabled' }"
            @click="setFilter('disabled')"
          >
            已禁用
          </div>
        </div>
      </div>

      <div class="user-list">
        <div v-for="user in filteredUsers" :key="user.userId" class="user-item">
          <div class="user-avatar">
            <i class="fas fa-user"></i>
          </div>
          <div class="user-info">
            <div class="user-name">{{ user.username }}</div>
            <div class="user-details">
              <span class="user-phone">{{ user.phone }}</span>
              <span class="user-email">{{ user.email }}</span>
            </div>
            <div class="user-status">
              <span class="status-badge" :class="{ 
                'status-enabled': !user.disabled, 
                'status-disabled': user.disabled 
              }">
                {{ user.disabled ? '已禁用' : '已启用' }}
              </span>
              <span class="register-date">注册时间：{{ user.registerDate }}</span>
            </div>
          </div>
          <div class="user-actions">
            <button 
              class="action-btn" 
              :class="{ 
                'enable-btn': user.disabled, 
                'disable-btn': !user.disabled 
              }"
              @click="toggleUserStatus(user)"
            >
              {{ user.disabled ? '启用' : '禁用' }}
            </button>
          </div>
        </div>
      </div>

      <div v-if="filteredUsers.length === 0" class="empty-state">
        <i class="fas fa-users"></i>
        <p>暂无用户数据</p>
      </div>

      <!-- 确认对话框 - 完全保留原始样式 -->
      <div v-if="showConfirmModal" class="modal-overlay" @click.self="showConfirmModal = false">
        <div class="modal-content">
          <div class="modal-header">
            <h3>确认操作</h3>
            <span class="close-btn" @click="showConfirmModal = false">&times;</span>
          </div>
          <div class="modal-body">
            <p>确定要{{ selectedUser?.disabled ? '启用' : '禁用' }}用户 "{{ selectedUser?.username }}" 吗？</p>
          </div>
          <div class="modal-footer">
            <button class="modal-btn confirm-btn" @click="confirmToggle">确认</button>
            <button class="modal-btn cancel-btn" @click="showConfirmModal = false">取消</button>
          </div>
        </div>
      </div>
      <AdminFooter />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';
import { toast } from '../utils/toast';
import AdminFooter from '@/components/AdminFooter.vue';

// 路由实例
const router = useRouter();

// 状态定义（完全对应原data）
const searchKeyword = ref('');
const activeFilter = ref('all');
const showConfirmModal = ref(false);
const selectedUser = ref(null);
const users = ref([]);

// 计算属性（保持原逻辑）
const filteredUsers = computed(() => {
  let filtered = users.value;
  
  // 按状态过滤
  if (filterStatus.value === 1) {
    filtered = filtered.filter(user => !user.disabled);
  } else if (filterStatus.value === 2) {
    filtered = filtered.filter(user => user.disabled);
  }
  
  // 按关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase();
    filtered = filtered.filter(user => 
      user.username.toLowerCase().includes(keyword) ||
      user.phone.includes(keyword) ||
      user.email.toLowerCase().includes(keyword)
    );
  }
  
  return filtered;
});

const filterStatus = computed(() => {
  return activeFilter.value === 'all' ? 0 : 
         activeFilter.value === 'enabled' ? 1 : 2;
});

const getPersonList = async () => {
  try {
    // 打印请求参数
    console.log('请求状态：', filterStatus.value);
    const res = await request.get('/api/persons', {
      params: { status: filterStatus.value }
    });
    
    // 打印接口返回结果
    console.log('接口返回：', res);
    
    if (res.success) {
      // 正确赋值给 Vue3 的响应式变量
      users.value = res.data.map(item => ({
        userId: item.id,
        username: item.username,
        phone: item.phone || '未填写',
        email: item.email || '未填写',
        disabled: !item.activated,
        registerDate: formatDate(item.createTime),
        photo: item.photo || ''
      }));
    } else {
      toast.error(`获取失败：${res.message}`);
    }
  } catch (error) {
    console.error('获取用户列表失败：', error);
    // 区分网络错误和接口错误
    if (error.response) {
      toast.error(`接口错误：${error.response.status} ${error.response.statusText}`);
    } else if (error.request) {
      toast.error('网络错误，无法连接到服务器');
    } else {
      toast.error('请求失败：' + error.message);
    }
  }
};

const handleSearch = async () => {
  try {
    const res = await request.post('/api/persons/search', {
      keyword: searchKeyword.value,
      status: filterStatus.value
    });
    if (res.success) {
      users.value = res.data.map(item => ({
        userId: item.id,
        username: item.username,
        phone: item.phone || '未填写',
        email: item.email || '未填写',
        disabled: !item.activated,
        registerDate: formatDate(item.createTime),
        photo: item.photo || ''
      }));
    }
  } catch (error) {
    console.error('搜索用户失败：', error);
    toast.error('搜索失败，请重试');
  }
};

const confirmToggle = async () => {
  if (!selectedUser.value) return;

  try {
    const username = selectedUser.value.username;
    const targetActivated = selectedUser.value.disabled;
    
    await request.put(`/api/${username}/status`, null, {
      params: { activated: targetActivated }
    });

    selectedUser.value.disabled = !selectedUser.value.disabled;
    showConfirmModal.value = false;
    toast.success(`用户已${selectedUser.value.disabled ? '禁用' : '启用'}`);
  } catch (error) {
    console.error('切换用户状态失败：', error);
    toast.error('操作失败，请重试');
  }
};

const formatDate = (dateStr) => {
  if (!dateStr) return '未知时间';
  const date = new Date(dateStr);
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
};

const goBack = () => {
  router.back();
};

const setFilter = (filter) => {
  activeFilter.value = filter;
  getPersonList();
};

const toggleUserStatus = (user) => {
  selectedUser.value = user;
  showConfirmModal.value = true;
};

const setActiveNav = (navKey) => {
  activeNav.value = navKey;
};

// 页面挂载时执行
onMounted(() => {
  getPersonList();
});
</script>

<style scoped>
/* 完全保留原始样式代码，未做任何修改 */
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.3/css/all.min.css');

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: 'Helvetica Neue', Arial, sans-serif;
}

.manage-user-container {
  background-color: #f5f5f5;
  color: #333;
  line-height: 1.6;
  padding: 0;
  max-width: 100%;
  overflow-x: hidden;
  padding-bottom: 0;
}

.container {
  /* max-width: 600px; */
  margin: 0 auto;
  background: #fff;
  min-height: 100vh;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-radius: 16px;
  padding-bottom: 5vh;
  padding-left: 0;
  padding-right: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  padding-top: 150px;
}

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

.top-back-btn {
  position: absolute;
  top: 15px;
  left: 15px;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: none;
  border-radius: 20px;
  padding: 8px 15px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 5px;
  z-index: 10;
}

.top-back-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.search-section {
  width: 92%;
  max-width: 500px;
  margin-top: -2vw;
  transform: translateY(-40px);
  position: fixed;
  z-index:999;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
  background: #f8f9fad8;
  border-radius: 25px;
  border:1px solid #f0f0f0;
  padding: 12px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.search-box i {
  color: #999;
  margin-right: 10px;
}

.search-box input {
  border: none;
  background: transparent;
  outline: none;
  flex: 1;
  font-size: 1rem;
  color: #333;
}

.search-box input::placeholder {
  color: #999;
}

.filter-section {
  width: 92%;
  max-width: 500px;
  margin-top: 40px;
  transform: translateY(-40px);
  position: fixed;
  z-index:999;
}

.filter-tabs {
  display: flex;
  background: #f8f9fa;
  border-radius: 12px;
  padding: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.filter-tab {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #666;
  border-radius: 8px;
}

.filter-tab.active {
  color: #0097ff;
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.user-list {
  width: 92%;
  max-width: 500px;
  display: flex;
  flex-direction: column;
  gap: 15px;
  transform: translateY(-40px);
  margin-top:27vw;
}

.user-item {
  display: flex;
  align-items: center;
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
}

.user-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.user-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: #f8f9fa;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  flex-shrink: 0;
}

.user-avatar i {
  font-size: 1.5rem;
  color: #666;
}

.user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.user-name {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.user-phone, .user-email {
  font-size: 0.9rem;
  color: #666;
}

.user-status {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 5px;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 500;
}

.status-enabled {
  background: #e8f5e8;
  color: #4caf50;
}

.status-disabled {
  background: #ffeaea;
  color: #f44336;
}

.register-date {
  font-size: 0.8rem;
  color: #999;
}

.user-actions {
  display: flex;
  align-items: center;
}

.action-btn {
  border: none;
  border-radius: 20px;
  padding: 8px 16px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.enable-btn {
  background: #4caf50;
  color: white;
}

.enable-btn:hover {
  background: #45a049;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
}

.disable-btn {
  background: #e15656;
  color: white;
  border: 1px solid #f3caca;
}

.disable-btn:hover {
  background: #d32f2f;
  box-shadow: 0 4px 12px rgba(225, 86, 86, 0.3);
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-state i {
  font-size: 3rem;
  margin-bottom: 15px;
  color: #ddd;
}

.empty-state p {
  font-size: 1.1rem;
}




.modal-content {
  background: white;
  border-radius: 12px;
  padding: 20px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  gap: 15px;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: scale(0.95) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.2rem;
  color: #333;
}

.close-btn {
  font-size: 1.5rem;
  color: #aaa;
  cursor: pointer;
  transition: color 0.2s;
}

.close-btn:hover {
  color: #666;
}

.modal-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.modal-body p {
  color: #555;
  line-height: 1.5;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 10px;
  border-top: 1px solid #eee;
}

.modal-btn {
  border: none;
  border-radius: 20px;
  padding: 10px 20px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  transition: all 0.3s ease;
}

.cancel-btn {
  background-color: #e0e0e0;
  color: #333;
}

.cancel-btn:hover {
  background-color: #c7c7c7;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.confirm-btn {
  background-color: #1e80ff;
  color: white;
}

.confirm-btn:hover {
  background-color: #0085e0;
  box-shadow: 0 4px 12px rgba(30, 128, 255, 0.3);
}

@media (max-width: 480px) {
  .container {
    max-width: 100vw;
    width: 100vw;
    border-radius: 0;
    padding-top: 150px;
    padding-bottom: 65px;
  }
  
  .top-background {
    height: 90px;
    border-radius: 0;
    max-width: 100vw;
  }
  
  .user-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }
  
  .user-actions {
    width: 100%;
    justify-content: flex-end;
  }
  
  .user-details {
    flex-direction: column;
  }
}
</style>
