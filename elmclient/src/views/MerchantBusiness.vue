<template>
  <div class="shop-management-page">
    <div class="top-background">
      <h1>经营工作台</h1>
    </div>

    <div class="workbench-summary">
      <div><strong>{{ shops.length }}</strong><span>我的店铺</span></div>
      <div><strong>{{ approvedCount }}</strong><span>已上线</span></div>
      <div><strong>{{ pendingCount }}</strong><span>待审核</span></div>
    </div>

    <div class="status-tabs">
      <button v-for="tab in tabs" :key="tab.status" :class="{ active: activeTab === tab.status }"
        @click="changeTab(tab.status)">
        {{ tab.label }}
      </button>
    </div>

    <div class="container wrapper">
      <ul class="business-list">
        <li v-for="(shop, index) in filteredShops" :key="shop?.id || index">
          <!-- <div class="status-badge" :class="getStatusClass(shop.status)">
            {{ getStatusText(shop.status) }}
          </div> -->
          <img :src="shop?.businessImg || require('@/assets/business-default.png')" :alt="shop?.businessName || '未命名商铺'"
            class="logo" @error="handleImageError" />
          <div class="business-info-detail">
            <h3>{{ shop?.businessName || '未命名商铺' }}</h3>
            <div class="delivery-info-container">
              <div class="business-info-delivery">
                <p>配送费{{ shop?.deliveryPrice?.toFixed(2) || '0.00' }}元</p>
              </div>
              <div class="business-info-delivery">
                <p>起送费{{ shop?.startPrice?.toFixed(2) || '0.00' }}元</p>
              </div>
            </div>
            <div class="business-info-delivery">
              <p>商家地址：{{ shop?.businessAddress || '暂无地址信息' }}</p>
            </div>
            <div class="shop-tags">
              <span :class="shop?.operatingStatus === false ? 'shop-closed' : 'shop-open'">{{ shop?.operatingStatus === false ? '休息中' : '营业中' }}</span>
              <span v-if="shop?.dineInAvailable">堂食店</span>
              <span v-if="shop?.promotionThreshold && shop?.promotionDiscount">满{{ Number(shop.promotionThreshold).toFixed(0) }}减{{ Number(shop.promotionDiscount).toFixed(0) }}</span>
            </div>
          </div>
          <div class="action-buttons">
            <button class="edit-btn" @click="editShop(shop?.id || index)" :disabled="shop.status === 2">编辑</button>
            <button class="delete-btn" @click="deleteShop(shop?.id || index)">删除</button>
          </div>
        </li>
      </ul>
    </div>

    <div class="footer-button-container">
      <button class="apply-button" @click="applyNewShop">申请新店</button>
    </div>

  </div>
</template>

<script>
import Swal from 'sweetalert2';
import { ref, onMounted, computed } from 'vue';
import request from '../utils/request';
import { useRouter } from 'vue-router';
import { toast } from '../utils/toast';
import { clearAuth, getToken } from '../utils/auth';
import { listMyBusinesses } from '../services/businessService';

export default {
  name: 'MyApplication',
  setup() {
    const router = useRouter();
    const shops = ref([]);
    const loading = ref(false);
    const errorMessage = ref('');
    const activeTab = ref(null);

    const tabs = [
      { status: null, label: '全部' },
      { status: 0, label: '审核中' },
      { status: 1, label: '已上线' },
      { status: 2, label: '未通过' }
    ];

    const getStatusText = (status) => {
      switch (status) {
        case 0: return '审核中';
        case 1: return '已上线';
        case 2: return '审核未通过';
        default: return '未知状态';
      }
    };

    const getStatusClass = (status) => {
      switch (status) {
        case 0: return 'status-pending';
        case 1: return 'status-approved';
        case 2: return 'status-rejected';
        default: return '';
      }
    };

    // 外部对象存储图片可能失效，工作台始终保留本地兜底图，避免出现破图图标和 alt 文本。
    const handleImageError = (event) => {
      const image = event?.target;
      if (!image || image.dataset.fallbackApplied === 'true') return;
      image.dataset.fallbackApplied = 'true';
      image.src = require('@/assets/business-default.png');
    };

    const filteredShops = computed(() => {
      if (activeTab.value === null) {
        return shops.value;
      }
      return shops.value.filter(shop => shop.status === activeTab.value);
    });
    const approvedCount = computed(() => shops.value.filter(shop => shop.status === 1).length);
    const pendingCount = computed(() => shops.value.filter(shop => shop.status === 0).length);

    const changeTab = (status) => {
      activeTab.value = status;
      loadShops(status);
    };

    const loadShops = async (status = null) => {
      loading.value = true;
      errorMessage.value = '';
      try {
        const token = getToken();
        if (!token) {
          toast.warning('用户未登录，请先登录！');
          router.push({ path: '/login' });
          return;
        }
        shops.value = await listMyBusinesses(status);
      } catch (error) {
        console.error('获取商铺列表失败:', error);
        if (error.response && error.response.status === 401) {
          toast.error('登录已过期，请重新登录！');
          clearAuth();
          router.push({ path: '/login' });
        } else {
          errorMessage.value = '获取商铺列表失败，请重试！';
          toast.error('获取商铺列表失败，请重试！');
        }
      } finally {
        loading.value = false;
      }
    };

    const deleteShop = async (shopId) => {
      const result = await Swal.fire({
        title: '确定删除此店铺？',
        text: "删除后将无法恢复！",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      });
      if (result.isConfirmed) {
        try {
          const token = getToken();
          if (!token) {
            toast.warning('用户未登录，请先登录！');
            router.push({ path: '/login' });
            return;
          }
          await request.delete(`/api/businesses/${shopId}`, { headers: { 'Authorization': `Bearer ${token}` } });
          shops.value = shops.value.filter(shop => shop.id !== shopId);
          toast.success('店铺删除成功！');
        } catch (error) {
          console.error('删除店铺失败:', error);
          toast.error('删除店铺失败，请重试！');
        }
      }
    };

    const editShop = (shopId) => {
      router.push(`/merchant/businessinfo?businessId=${shopId}`);
    };

    const applyNewShop = async () => {
      try {
        const token = getToken();
        if (!token) {
          toast.warning('用户未登录，请先登录！');
          router.push({ path: '/login' });
          return;
        }

        let uploadedFile = null;
        let imageUrl = '';
        let selectedOrderType = null;

        const { value: formValues } = await Swal.fire({
          title: '申请新店',
          html: `
            <div class="swal2-content-wrapper">
              <div id="image-upload-area" class="image-upload-area">
                <span id="upload-icon" class="upload-icon">+</span>
                <span id="upload-text" class="upload-text">上传商铺图片</span>
                <img id="image-preview" src="" class="image-preview"/>
                <input id="businessImg" type="file" accept="image/*" class="file-input">
              </div>
              <input id="businessName" class="swal2-input modern-input" placeholder="商铺名称" required>
              <input id="businessAddress" class="swal2-input modern-input" placeholder="商铺地址" required>
              <textarea id="businessExplain" class="swal2-textarea modern-textarea" placeholder="商铺介绍"></textarea>
              <input id="deliveryPrice" class="swal2-input modern-input" placeholder="配送费(元)" type="number" min="0" step="0.1" required>
              <input id="startPrice" class="swal2-input modern-input" placeholder="起送价(元)" type="number" min="0" step="0.1" required>
              <label class="shop-setting-row"><input id="dineInAvailable" type="checkbox"><span>支持堂食</span><small>在首页显示“堂食店”</small></label>
              <select id="promotionPreset" class="swal2-input modern-input">
                <option value="">不设置满减</option>
                <option value="20-3">满20减3</option>
                <option value="30-5">满30减5</option>
                <option value="50-10">满50减10</option>
              </select>
              
              <div class="input-with-button-container">
                <input id="orderTypeInput" class="swal2-input modern-input" placeholder="请选择商铺类型" readonly required>
                <button id="selectTypeBtn" class="select-type-btn">选择</button>
              </div>
            </div>
          `,
          width: '90%',
          maxWidth: '450px',
          padding: '1rem',
          focusConfirm: false,
          showCancelButton: true,
          confirmButtonText: '提交申请',
          confirmButtonColor: '#0097ff',
          cancelButtonText: '取消',
          customClass: {
            popup: 'compact-popup',
            content: 'compact-content'
          },
          didOpen: () => {
            const fileInput = document.getElementById('businessImg');
            const imagePreview = document.getElementById('image-preview');
            const uploadIcon = document.getElementById('upload-icon');
            const uploadText = document.getElementById('upload-text');

            const orderTypeInput = document.getElementById('orderTypeInput');
            const selectTypeBtn = document.getElementById('selectTypeBtn');

            // 实时校验函数
            const validateField = (inputId, validationFn, errorMessage) => {
              const input = document.getElementById(inputId);
              if (input) {
                input.addEventListener('input', () => {
                  const isValid = validationFn(input.value.trim());
                  if (!isValid) {
                    input.style.borderColor = '#dc3545';
                    input.style.backgroundColor = '#fff5f5';
                    // 显示错误提示
                    showFieldError(input, errorMessage);
                  } else {
                    input.style.borderColor = '#28a745';
                    input.style.backgroundColor = '#f8fff8';
                    hideFieldError(input);
                  }
                });
              }
            };

            // 显示字段错误提示
            const showFieldError = (input, message) => {
              hideFieldError(input);
              const errorDiv = document.createElement('div');
              errorDiv.className = 'field-error-message';
              errorDiv.textContent = message;
              errorDiv.style.color = '#dc3545';
              errorDiv.style.fontSize = '12px';
              errorDiv.style.marginTop = '4px';
              input.parentNode.appendChild(errorDiv);
            };

            // 隐藏字段错误提示
            const hideFieldError = (input) => {
              const existingError = input.parentNode.querySelector('.field-error-message');
              if (existingError) {
                existingError.remove();
              }
            };

            // 商铺名称校验（最多10个字符）
            validateField('businessName', (value) => {
              return value.length <= 10;
            }, '商铺名称不能超过10个字符');

            // 商铺地址校验（最多15个字符）
            validateField('businessAddress', (value) => {
              return value.length <= 15;
            }, '商铺地址不能超过15个字符');

            // 商铺介绍校验（最多15个字符）
            validateField('businessExplain', (value) => {
              return value.length <= 15;
            }, '商铺介绍不能超过15个字符');

            // 配送费校验
            validateField('deliveryPrice', (value) => {
              if (!value) return false;
              const num = parseFloat(value);
              if (isNaN(num) || num < 0) return false;
              if (value.includes('.') && value.split('.')[1].length > 2) return false;
              return true;
            }, '配送费必须大于等于0，小数点最多保留两位');

            // 起送价校验
            validateField('startPrice', (value) => {
              if (!value) return false;
              const num = parseFloat(value);
              if (isNaN(num) || num < 0) return false;
              if (value.includes('.') && value.split('.')[1].length > 2) return false;
              return true;
            }, '起送价必须大于等于0，小数点最多保留两位');

            fileInput.addEventListener('change', (event) => {
              uploadedFile = event.target.files[0];
              if (uploadedFile) {
                const reader = new FileReader();
                reader.onload = (e) => {
                  imagePreview.src = e.target.result;
                  imagePreview.style.display = 'block';
                  uploadIcon.style.display = 'none';
                  uploadText.style.display = 'none';
                };
                reader.readAsDataURL(uploadedFile);
              } else {
                imagePreview.style.display = 'none';
                uploadIcon.style.display = 'block';
                uploadText.style.display = 'block';
              }
            });

            // 点击选择按钮，弹出类型选择弹窗
            selectTypeBtn.addEventListener('click', async (e) => {
              e.preventDefault();

              const typeOptions = [
                { id: 1, label: '美食' },
                { id: 2, label: '早餐' },
                { id: 3, label: '跑腿代购' },
                { id: 4, label: '汉堡披萨' },
                { id: 5, label: '甜品饮品' },
                { id: 6, label: '速食简食' },
                { id: 7, label: '地方小吃' },
                { id: 8, label: '米粉面馆' },
                { id: 9, label: '包子粥铺' },
                { id: 10, label: '炸鸡炸串' },
              ];

              const optionsHtml = typeOptions.map(option => `
                <div class="type-option" data-id="${option.id}">${option.label}</div>
              `).join('');

              // 创建一个新的弹窗，但不关闭原弹窗
              const typeSelectResult = await new Promise((resolve) => {
                // 创建覆盖层和弹窗
                const overlay = document.createElement('div');
                overlay.className = 'type-select-overlay';
                overlay.innerHTML = `
                  <div class="type-select-popup">
                    <div class="type-select-popup-header">
                      <h3>选择商铺类型</h3>
                      <button class="type-select-close">×</button>
                    </div>
                    <div class="type-options-container">${optionsHtml}</div>
                  </div>
                `;

                document.body.appendChild(overlay);

                // 添加事件监听
                const closeBtn = overlay.querySelector('.type-select-close');
                const options = overlay.querySelectorAll('.type-option');

                closeBtn.addEventListener('click', () => {
                  overlay.remove();
                  resolve(null);
                });

                options.forEach(option => {
                  option.addEventListener('click', () => {
                    const id = option.getAttribute('data-id');
                    const label = option.textContent;
                    overlay.remove();
                    resolve({ id: parseInt(id), label: label });
                  });
                });

                // 点击覆盖层关闭
                overlay.addEventListener('click', (e) => {
                  if (e.target === overlay) {
                    overlay.remove();
                    resolve(null);
                  }
                });
              });

              // 如果用户选择了类型，更新输入框
              if (typeSelectResult) {
                orderTypeInput.value = typeSelectResult.label;
                selectedOrderType = typeSelectResult;
              }
            });
          },
          preConfirm: async () => {
            const businessName = document.getElementById('businessName').value.trim();
            const businessAddress = document.getElementById('businessAddress').value.trim();
            const businessExplain = document.getElementById('businessExplain').value.trim();
            const deliveryPriceStr = document.getElementById('deliveryPrice').value.trim();
            const startPriceStr = document.getElementById('startPrice').value.trim();
            const dineInAvailable = document.getElementById('dineInAvailable').checked;
            const promotionValue = document.getElementById('promotionPreset').value;

            // 基础必填项校验
            if (!businessName || !businessAddress || !selectedOrderType) {
              Swal.showValidationMessage('请填写所有必填项');
              return false;
            }

            // 检查是否有字段校验错误
            const errorMessages = document.querySelectorAll('.field-error-message');
            if (errorMessages.length > 0) {
              Swal.showValidationMessage('请修正表单中的错误');
              return false;
            }

            const deliveryPrice = parseFloat(deliveryPriceStr) || 0;
            const startPrice = parseFloat(startPriceStr) || 0;
            const [promotionThreshold, promotionDiscount] = promotionValue
              ? promotionValue.split('-').map(value => parseFloat(value))
              : [null, null];

            if (uploadedFile) {
              const formData = new FormData();
              formData.append('file', uploadedFile);
              try {
                const uploadResponse = await request.post('/upload', formData, {
                  headers: {
                    'Content-Type': 'multipart/form-data',
                    'Authorization': `Bearer ${token}`
                  }
                });
                if (uploadResponse && uploadResponse.success && uploadResponse.data) {
                  imageUrl = uploadResponse.data;
                } else {
                  Swal.showValidationMessage(uploadResponse?.message || '图片上传失败');
                  return false;
                }
              } catch (error) {
                console.error('图片上传出错:', error.response || error);
                Swal.showValidationMessage(error.response?.data?.message || error.message || '图片上传出错');
                return false;
              }
            }
            return {
              businessName,
              businessAddress,
              businessExplain,
              deliveryPrice,
              startPrice,
              dineInAvailable,
              promotionThreshold,
              promotionDiscount,
              businessImg: imageUrl,
              orderTypeId: selectedOrderType.id
            };
          }
        });

        if (formValues) {
          const response = await request.post('/api/permission/apply-shop', formValues, {
            headers: { 'Authorization': `Bearer ${token}` }
          });
          if (response && response.success) {
            toast.success('新店申请提交成功！');
            await loadShops(activeTab.value);
          } else {
            toast.error(response?.message || '申请提交失败');
          }
        }
      } catch (error) {
        console.error('申请新店出错:', error);
        toast.error(error.response?.data?.message || '申请新店过程中出错，请重试');
      }
    };

    onMounted(() => {
      changeTab(null);
    });

    return {
      shops,
      loading,
      errorMessage,
      tabs,
      activeTab,
      filteredShops,
      approvedCount,
      pendingCount,
      deleteShop,
      editShop,
      applyNewShop,
      getStatusText,
      getStatusClass,
      handleImageError,
      changeTab
    };
  }
};
</script>

<style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css');

/* ----------------------- 基础样式 ----------------------- */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
  background-color: #f8f8f8;
  color: #333;
  line-height: 1.6;
}

/* 确保父容器居中，并让子元素继承或对齐到中心 */
.swal2-content-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  /* 关键：这会将内部的块级元素（如input）居中 */
}

/* 调整输入框和文本域的样式以适应居中对齐 */
.modern-input,
.modern-textarea,
.image-upload-area,
.input-with-button-container {
  width: 90%;
  /* 确保它们有足够的宽度，但不至于撑满容器 */
  max-width: 350px;
  /* 减小最大宽度 */
  margin: 8px 0;
  /* 减小上下间距 */
}

/* 可选：为选择按钮容器添加居中样式 */
.input-with-button-container {
  display: flex;
  justify-content: center;
  /* 确保内部元素居中 */
  align-items: center;
}

/* ----------------------- 顶部标题栏 ----------------------- */
.shop-management-page .top-background {
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

.shop-management-page .top-background::before {
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

/* ----------------------- 状态标签栏 ----------------------- */
.status-tabs {
  display: flex;
  justify-content: space-around;
  padding: 10px 0;
  background-color: #fff;
  border-bottom: 1px solid #eee;
  position: fixed;
  top: 100px;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 600px;
  z-index: 999;
}

.status-tabs button {
  padding: 8px 12px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  border-radius: 4px;
  transition: all 0.3s;
}

.status-tabs button.active {
  color: #0097ff;
  background-color: #e6f2ff;
  font-weight: bold;
}

.status-tabs button:hover {
  background-color: #f0f0f0;
}

/* ----------------------- 店铺列表 ----------------------- */
.container {
  max-width: 600px;
  margin: 0 auto;
  padding: 0 4vw;
  padding-top: 150px;
  /* 为固定的头部和标签栏留出空间 */
  padding-bottom: 140px;
}

.wrapper .business-list {
  width: 100%;
  padding: 0;
  margin: 15px 0;
  list-style: none;
}

.wrapper .business-list li {
  padding: 12px;
  background-color: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  transition: transform 0.3s ease-in-out;
}

.wrapper .business-list li:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.logo {
  width: 20vw;
  height: 20vw;
  max-width: 80px;
  max-height: 80px;
  object-fit: cover;
  border-radius: 8px;
  margin-right: 3vw;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.business-info-detail {
  flex: 1;
}

.business-info-detail h3 {
  font-size: clamp(16px, 4vw, 20px);
  margin: 0 0 8px 0;
  color: #333;
  font-weight: 600;
}

.business-info-delivery {
  font-size: clamp(14px, 3.5vw, 16px);
  color: #666;
  margin: 4px 0;
  display: flex;
}

.delivery-info-container {
  display: flex;
  gap: 10px;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-left: 10px;
}

.action-buttons button {
  background-color: #fff;
  border: 1px solid #ccc;
  border-radius: 6px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: clamp(12px, 3.5vw, 14px);
  transition: all 0.3s;
  white-space: nowrap;
}

.action-buttons button.edit-btn {
  color: #0097ff;
  border-color: #0097ff;
}

.action-buttons button.delete-btn {
  color: #dc3545;
  border-color: #dc3545;
}

.action-buttons button:hover {
  color: #fff;
}

.action-buttons button.edit-btn:hover {
  background-color: #0097ff;
}

.action-buttons button.delete-btn:hover {
  background-color: #dc3545;
}

.action-buttons button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background-color: #f0f0f0;
  color: #999;
}

/* ----------------------- 状态标签 ----------------------- */
.status-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
  color: white;
  z-index: 1;
}

.status-pending {
  background-color: #ffc107;
  /* 黄色，表示审核中 */
}

.status-approved {
  background-color: #28a745;
  /* 绿色，表示已上线 */
}

.status-rejected {
  background-color: #dc3545;
  /* 红色，表示审核未通过 */
}

/* ----------------------- 底部按钮 ----------------------- */
.footer-button-container {
  position: fixed;
  bottom: 80px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  padding: 0 4vw;
  box-sizing: border-box;
  z-index: 99;
}

.apply-button {
  width: 100%;
  max-width: 500px;
  background-color: #0097ff;
  color: #fff;
  padding: 12px 0;
  border-radius: 10px;
  text-decoration: none;
  font-size: clamp(16px, 4.5vw, 18px);
  font-weight: bold;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  border: none;
  cursor: pointer;
  transition: background-color 0.3s, transform 0.2s;
}

.apply-button:hover {
  background-color: #007bb5;
  transform: translateY(-2px);
}

.apply-button:active {
  transform: translateY(0);
}

/* ----------------------- 底部导航栏 ----------------------- */
.footer-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-around;
  align-items: center;
  height: 60px;
  background-color: #fff;
  border-top: 1px solid #f0f0f0;
  z-index: 100;
}

.footer-nav .nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #666;
  text-decoration: none;
  font-size: 12px;
  flex-grow: 1;
  text-align: center;
  padding: 8px 0;
}

.footer-nav .nav-item i {
  font-size: 20px;
  margin-bottom: 4px;
}

.footer-nav .nav-item.active {
  color: #0097ff;
}

.workbench-summary {
  width: min(92%, 560px);
  margin: 18px auto 0;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.workbench-summary > div {
  padding: 14px 12px;
  background: #fff;
  border: 1px solid #e1edf8;
  border-radius: 8px;
  text-align: center;
}
.workbench-summary strong,.workbench-summary span { display: block; }
.workbench-summary strong { color: #173b60; font-size: 20px; }
.workbench-summary span { color: #8297aa; font-size: 11px; margin-top: 4px; }
.workbench-summary { margin-top: 154px; }
.shop-management-page > .container { padding-top: 18px; }
@media (max-width: 480px) {
  .workbench-summary { margin-top: 142px; }
  .shop-management-page > .container { padding-top: 18px; }
}

/* 加载状态 */
.loading {
  text-align: center;
  padding: 20px;
  color: #666;
}

.error-message {
  color: #dc3545;
  text-align: center;
  padding: 15px;
  background-color: #f8d7da;
  border-radius: 6px;
  margin: 15px;
}

/* ----------------------- SweetAlert2 表单现代化样式 ----------------------- */
.swal2-content-wrapper {
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  /* 减小表单项之间的间距 */
}

/* 实时校验错误提示样式 */
.field-error-message {
  color: #dc3545 !important;
  font-size: 12px !important;
  margin-top: 4px !important;
  margin-bottom: 8px !important;
  padding: 4px 8px !important;
  background-color: #fff5f5 !important;
  border: 1px solid #fecaca !important;
  border-radius: 4px !important;
  animation: slideDown 0.3s ease-out !important;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 紧凑型弹窗样式 */
.compact-popup {
  font-size: 14px !important;
}

.compact-popup .swal2-title {
  font-size: 20px !important;
  margin-bottom: 15px !important;
}

.compact-content {
  padding: 0 !important;
}

.image-upload-area {
  position: relative;
  width: 120px;
  height: 120px;
  border: 2px dashed #0097ff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: border-color 0.3s, background-color 0.3s;
  margin: 0 auto 15px;
  background-color: #f7f7f7;
  /* 增加背景色 */
}

.image-upload-area:hover {
  border-color: #007bb5;
  background-color: #f0f8ff;
  /* 悬停时颜色变浅 */
}

.upload-icon {
  font-size: 40px;
  color: #0097ff;
  font-weight: 300;
  transition: all 0.3s;
}

.upload-text {
  font-size: 12px;
  color: #666;
  margin-top: 5px;
  transition: all 0.3s;
}

.image-preview {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
  display: none;
}

.file-input {
  position: absolute;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

.modern-input,
.modern-textarea {
  border: 1px solid #e0e0e0 !important;
  border-radius: 6px !important;
  padding: 10px 14px !important;
  transition: border-color 0.2s, box-shadow 0.2s !important;
  width: 100% !important;
  box-sizing: border-box !important;
  font-size: 14px !important;
  color: #333 !important;
}

.modern-input:focus,
.modern-textarea:focus {
  border-color: #0097ff !important;
  box-shadow: 0 0 0 4px rgba(0, 151, 255, 0.15) !important;
  outline: none !important;
}

.modern-input::placeholder,
.modern-textarea::placeholder {
  color: #999 !important;
  font-style: italic;
}

/* 新增的样式 */
.input-with-button-container {
  display: flex;
  align-items: center;
  gap: 10px;
  /* 输入框和按钮之间的间距 */
  width: 100%;
}

.input-with-button-container .modern-input {
  flex-grow: 1;
  /* 让输入框占据剩余空间 */
  cursor: pointer;
  /* 增加手型光标 */
  color: #666;
  /* 默认文字颜色 */
}

.select-type-btn {
  padding: 10px 14px;
  border: 1px solid #0097ff;
  background-color: #0097ff;
  color: #fff;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.3s, transform 0.2s;
  white-space: nowrap;
  /* 防止按钮文字换行 */
}

.select-type-btn:hover {
  background-color: #007bb5;
  border-color: #007bb5;
  transform: translateY(-1px);
}

.select-type-btn:active {
  transform: translateY(0);
}

/* 弹出类型选择列表的样式 */
.type-select-popup .swal2-popup {
  width: 320px !important;
  /* 调整弹窗宽度 */
  padding: 0 !important;
}

.type-options-container {
  display: flex;
  flex-direction: column;
  padding: 10px;
}

.type-option {
  padding: 15px;
  text-align: center;
  font-size: 16px;
  color: #333;
  cursor: pointer;
  transition: background-color 0.2s;
  border-radius: 6px;
}

.type-option:hover {
  background-color: #f0f0f0;
}

/* ----------------------- 类型选择覆盖层弹窗样式 ----------------------- */
.type-select-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 10000;
  animation: fadeIn 0.2s ease-out;
}

.type-select-popup {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  width: 90%;
  max-width: 320px;
  max-height: 70vh;
  overflow: hidden;
  animation: slideIn 0.3s ease-out;
}

.type-select-popup-header {
  padding: 16px 20px;
  background-color: #f8f9fa;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.type-select-popup-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.type-select-close {
  background: none;
  border: none;
  font-size: 24px;
  color: #666;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 50%;
  transition: background-color 0.2s, color 0.2s;
}

.type-select-close:hover {
  background-color: #e9ecef;
  color: #333;
}

.type-options-container {
  max-height: 400px;
  overflow-y: auto;
  padding: 8px 0;
}

.type-option {
  padding: 14px 20px;
  cursor: pointer;
  transition: background-color 0.2s;
  font-size: 16px;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
}

.type-option:last-child {
  border-bottom: none;
}

.type-option:hover {
  background-color: #f8f9fa;
  color: #0097ff;
}

.type-option:active {
  background-color: #e6f2ff;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.95);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* ----------------------- 移动端响应式样式 ----------------------- */
@media (max-width: 480px) {
  .shop-management-page {
    max-width: 100vw;
    width: 100vw;
  }

  .top-background {
    height: 90px;
    border-radius: 0;
    max-width: 100vw;
  }

  .status-tabs {
    top: 90px;
    max-width: 100vw;
    transform: none;
    left: 0;
  }

  .container {
    max-width: 100vw;
    width: 100vw;
    padding-top: 140px;
  }
}

/* 蓝白版工作台：收紧信息密度，避免移动端横向挤压 */
.shop-management-page {
  width: 100%;
  max-width: 600px;
  min-height: 100vh;
  margin: 0 auto;
  background: #f5f9fd;
  color: #24405c;
  overflow-x: hidden;
}
.shop-management-page .top-background {
  height: 64px;
  max-width: 600px;
  border-radius: 0;
  background: #0097ff;
  background-image: none;
  box-shadow: 0 1px 0 rgba(0, 83, 145, .15);
}
.shop-management-page .top-background::before { display: none; }
.shop-management-page .top-background h1 {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 0;
  text-shadow: none;
}
.shop-management-page .workbench-summary {
  width: calc(100% - 32px);
  margin: 118px auto 10px;
  gap: 8px;
}
.shop-management-page .workbench-summary > div {
  min-width: 0;
  padding: 12px 8px;
  border: 1px solid #dcebf7;
  border-radius: 8px;
  box-shadow: none;
}
.shop-management-page .workbench-summary strong { font-size: 18px; color: #1d537d; }
.shop-management-page .workbench-summary span { font-size: 12px; color: #6c8499; white-space: nowrap; }
.shop-management-page .status-tabs {
  top: 64px;
  height: 44px;
  padding: 4px 8px;
  border-bottom: 1px solid #dcebf7;
  box-shadow: none;
}
.shop-management-page .status-tabs button {
  min-width: 56px;
  padding: 7px 8px;
  font-size: 13px;
  color: #678198;
}
.shop-management-page .status-tabs button.active { color: #0879c7; background: #e8f5ff; }
.shop-management-page .container {
  width: calc(100% - 32px);
  max-width: 568px;
  padding: 0 0 132px;
}
.shop-management-page .business-list { margin: 0; }
.shop-management-page .business-list li {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 14px;
  margin-bottom: 10px;
  border: 1px solid #e1edf7;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(36, 91, 132, .06);
  transition: none;
}
.shop-management-page .business-list li:hover { transform: none; box-shadow: 0 2px 8px rgba(36, 91, 132, .06); }
.shop-management-page .logo {
  width: 56px;
  height: 56px;
  margin: 0;
  border-radius: 8px;
  box-shadow: none;
}
.shop-management-page .business-info-detail { min-width: 0; }
.shop-management-page .business-info-detail h3 {
  margin: 0 0 6px;
  color: #24405c;
  font-size: 16px;
  line-height: 1.35;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.shop-management-page .delivery-info-container { display: flex; flex-wrap: wrap; gap: 2px 12px; }
.shop-management-page .business-info-delivery {
  min-width: 0;
  margin: 2px 0;
  color: #71879a;
  font-size: 12px;
  line-height: 1.5;
}
.shop-management-page .business-info-delivery p { overflow-wrap: anywhere; }
.shop-management-page .action-buttons {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-left: 0;
}
.shop-management-page .action-buttons button {
  min-width: 52px;
  padding: 6px 8px;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.3;
  box-shadow: none;
}
.shop-management-page .action-buttons button.edit-btn { color: #0879c7; border-color: #9bcdf0; background: #f4fbff; }
.shop-management-page .action-buttons button.delete-btn { color: #b45a5a; border-color: #e7bcbc; background: #fffafa; }
.shop-management-page .footer-button-container { bottom: 72px; padding: 0 16px; }
.shop-management-page .apply-button { max-width: 568px; padding: 10px 0; border-radius: 7px; font-size: 15px; box-shadow: 0 3px 10px rgba(0, 116, 194, .18); }
.shop-management-page .footer-nav { height: 64px; border-top-color: #dcebf7; }
.shop-management-page .footer-nav .nav-item { padding: 7px 0; color: #7890a4; }
.shop-management-page .footer-nav .nav-item.active { color: #0097ff; }
@media (max-width: 480px) {
  .shop-management-page .top-background { height: 64px; }
  .shop-management-page .status-tabs { top: 64px; left: 0; transform: none; max-width: 100vw; }
  .shop-management-page .workbench-summary { margin-top: 118px; }
  .shop-management-page .container { width: calc(100% - 24px); padding-bottom: 132px; }
  .shop-management-page .business-list li { grid-template-columns: 52px minmax(0, 1fr) auto; gap: 10px; padding: 12px; }
  .shop-management-page .logo { width: 52px; height: 52px; }
}
.shop-setting-row {
  display: flex;
  align-items: center;
  gap: 7px;
  width: calc(100% - 1.5rem);
  margin: 8px auto;
  color: #45677d;
  font-size: 13px;
  text-align: left;
}
.shop-setting-row input { width: 16px; height: 16px; margin: 0; accent-color: #168bd1; }
.shop-setting-row small { margin-left: auto; color: #9aadb9; font-size: 11px; }
.shop-tags { display: flex; flex-wrap: wrap; gap: 5px; margin-top: 4px; }
.shop-tags span { padding: 2px 6px; border: 1px solid #cfe3f0; border-radius: 3px; background: #f5fbff; color: #168bd1; font-size: 11px; }
.shop-tags .shop-open{border-color:#bfe2cc;background:#f1faf4;color:#34895a}.shop-tags .shop-closed{border-color:#dce4e9;background:#f5f7f8;color:#7f8e99}

/* 申请新店是列表后的普通操作，不能固定覆盖正在浏览的店铺卡片。 */
.shop-management-page .footer-button-container {
  position: static;
  width: min(100% - 24px, 600px);
  margin: 0 auto 76px;
  padding: 16px 0 0;
}
.shop-management-page .container { padding-bottom: 8px; }
</style>
