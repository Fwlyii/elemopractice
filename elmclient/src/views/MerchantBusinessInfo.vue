<template>
  <div class="wrapper">
    <header>
      <div class="header-icon">
        <i class="fa fa-angle-left" @click="$router.back()"></i>
      </div>
      <p>商家信息</p>
      <div></div>
    </header>

    <div class="business-info-card">
      <div class="business-logo">
        <img :src="business.businessImg || require('@/assets/default-business.png')" @error="handleImageError" alt="商家图片" />
      </div>
      <div class="info-details">
        <h1>{{ business.businessName }}</h1>
        <p class="price-info">
          <span class="info-item">起送价 &#165;{{ business.startPrice }}</span>
          <span class="info-item">配送费 &#165;{{ business.deliveryPrice }}</span>
        </p>
        <p class="explain-text">{{ business.businessExplain }}</p>
        <div class="store-settings">
          <span :class="business.operatingStatus === false ? 'closed-setting' : 'open-setting'">{{ business.operatingStatus === false ? '休息中' : '营业中' }}</span>
          <span v-if="business.dineInAvailable">堂食店</span>
          <span v-if="business.promotionThreshold && business.promotionDiscount">满{{ Number(business.promotionThreshold).toFixed(0) }}减{{ Number(business.promotionDiscount).toFixed(0) }}</span>
          <span v-else class="muted-setting">未设置满减</span>
        </div>
      </div>
    </div>

    <div class="likes-collections">
      <div class="icon-item">
        <i class="fa fa-thumbs-up"></i>
        <span>点赞: {{ favoriteCount.likeCount }}</span>
      </div>
      <div class="icon-item">
        <i class="fa fa-bookmark"></i>
        <span>收藏: {{ favoriteCount.collectCount }}</span>
      </div>
    </div>

    <div class="edit-button-container">
      <button class="operating-button" :class="{ closed: business.operatingStatus === false }" @click="toggleOperatingStatus">{{ business.operatingStatus === false ? '开始营业' : '暂停营业' }}</button>
      <button class="edit-button" @click="showEditBusinessModal">编辑商家信息</button>
    </div>

    <ul class="food">
      <li v-for="(item, index) in foodArr" :key="item.foodId">
        <div class="food-left">
          <img :src="item.foodImg || require('@/assets/food-default.png')" @error="handleImageError" alt="商品图片" />
          <div class="food-left-info">
            <h3>{{ item.foodName }}</h3>
            <text class="food-status" v-if="item.shelveStatus === 0">(已下架)</text>
            <p>{{ item.foodExplain }}</p>
            <p class="food-category-note">分类：{{ item.category || '其他' }}<span v-if="item.purchaseLimit"> · 单笔最多 {{ item.purchaseLimit }} 份</span></p>
            <p class="food-price">&#165;{{ item.foodPrice }}<span v-if="Number(item.stock || 0) <= 0" class="sold-out-label">已售罄</span></p>
          </div>
        </div>
        <div class="food-right">
          <button class="action-button" @click="showEditFoodModal(item.id,index)">编辑</button>
          <button class="action-button shelve-button" @click="shelveFood(item.id,item.shelveStatus,index)">
            <div v-if="item.shelveStatus === 0">上架</div>
            <div v-else-if="item.shelveStatus === 1">下架</div>
          </button>
          <button class="action-button delete-button" @click="deleteFood(item.id,index)">删除</button>
        </div>
      </li>
    </ul>

    <div class="footer-button-container">
      <button class="add-food-button" @click="showAddNewFoodModal">添加商品</button>
    </div>

  </div>
</template>

<script>
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import Swal from 'sweetalert2';
import request from '../utils/request';
import businessDefaultImg from '@/assets/business-default.png';
import foodDefaultImg from '@/assets/food-default.png';

export default {
  name: "BusinessInfo",
  setup() {
    const route = useRoute();
    const businessId = ref();
    const business = ref({});
    const foodArr = ref([]);
    const favoriteCount = ref({});
    const escapeHtml = (value) => String(value ?? '').replace(/[&<>'"]/g, character => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
    })[character]);
    const categoryOptionsHtml = () => [...new Set(foodArr.value
      .map(item => String(item.category || '').trim())
      .filter(Boolean))]
      .map(category => `<option value="${escapeHtml(category)}"></option>`)
      .join('');
    const purchaseLimitFieldsHtml = (value = null) => {
      const limited = Number.isInteger(Number(value)) && Number(value) > 0;
      return `
        <label class="edit-field"><span>购买限制</span><select id="purchaseLimitMode" class="swal2-input">
          <option value="none" ${limited ? '' : 'selected'}>不限购</option>
          <option value="limited" ${limited ? 'selected' : ''}>每单限购</option>
        </select><small>限购数量会同步展示给顾客</small></label>
        <label id="purchaseLimitField" class="edit-field" style="${limited ? '' : 'display:none;'}"><span>每单最多</span><input id="purchaseLimit" type="number" min="1" max="999" class="swal2-input" placeholder="请输入 1～999" value="${limited ? escapeHtml(value) : ''}"></label>`;
    };
    const bindPurchaseLimitFields = () => {
      const mode = document.getElementById('purchaseLimitMode');
      const field = document.getElementById('purchaseLimitField');
      const input = document.getElementById('purchaseLimit');
      if (!mode || !field || !input) return;
      const sync = () => {
        const limited = mode.value === 'limited';
        field.style.display = limited ? '' : 'none';
        input.disabled = !limited;
        if (!limited) input.value = '';
      };
      mode.addEventListener('change', sync);
      sync();
    };
    const readPurchaseLimit = () => {
      if (document.getElementById('purchaseLimitMode')?.value !== 'limited') return null;
      const value = Number(document.getElementById('purchaseLimit')?.value);
      return Number.isInteger(value) && value >= 1 && value <= 999 ? value : Number.NaN;
    };
    const toggleOperatingStatus = async () => {
      const nextStatus = business.value.operatingStatus === false;
      const actionText = nextStatus ? '开始营业' : '暂停营业';
      const confirmation = await Swal.fire({
        title: actionText,
        text: nextStatus ? '开启后顾客可以正常下单。' : '暂停后仍展示店铺，但顾客暂时不能下单。',
        icon: 'question',
        showCancelButton: true,
        confirmButtonText: actionText,
        cancelButtonText: '取消'
      });
      if (!confirmation.isConfirmed) return;
      try {
        const response = await request.patch(`/api/businesses/own/${businessId.value}`, { operatingStatus: nextStatus });
        if (!response?.success) throw new Error(response?.message || '更新失败');
        business.value = response.data || { ...business.value, operatingStatus: nextStatus };
        Swal.fire({ icon: 'success', title: nextStatus ? '店铺已营业' : '店铺已暂停营业', timer: 1200, showConfirmButton: false });
      } catch (error) {
        Swal.fire('更新失败', error?.response?.data?.message || error?.message || '请稍后重试', 'error');
      }
    };

    onMounted(() => {
      businessId.value = parseInt(route.query.businessId);
      fetchBusinessBaseData();
      fetchBusinessFavoriteData();
      fetchBusinessFoodListData();
    });

    // 获取商铺基本信息
    const fetchBusinessBaseData = () => {
      request.get("/api/businesses/" + businessId.value)
        .then(response => {
          business.value = response.data;
        })
        .catch(error => {
          console.error('获取商铺信息失败:', error);
        });
    };

    // 获取商铺点赞收藏数据
    const fetchBusinessFavoriteData = () => {
      request.get("/api/merchant/interaction/stats/" + businessId.value)
        .then(response => {
          favoriteCount.value = response.data;
        })
        .catch(error => {
          console.error('获取点赞收藏数据失败:', error);
        });
    };

    // 获取商铺商品列表
    const fetchBusinessFoodListData = () => {
      request.get("/api/foods/list?businessId=" + businessId.value)
        .then(response => {
          foodArr.value = response.data;
        })
        .catch(error => {
          console.error('获取商品列表失败:', error);
        });
    };

    // 编辑商铺信息
    const showEditBusinessModal = async () => {
      let selectedFile = null;
      let currentImageUrl = business.value.businessImg;
      const currentOrderTypeId = business.value.orderTypeId || '';
      const currentDineInAvailable = Boolean(business.value.dineInAvailable);
      const currentPromotionValue = business.value.promotionThreshold && business.value.promotionDiscount
        ? `${business.value.promotionThreshold}-${business.value.promotionDiscount}` : '';

      const { value: formValues } = await Swal.fire({
        title: '编辑商铺信息',
        html: `
          <div style="text-align: center; margin-bottom: 15px;">
            <img id="image-preview" src="${escapeHtml(business.value.businessImg || businessDefaultImg)}" onerror="this.onerror=null;this.src='${escapeHtml(businessDefaultImg)}'" style="max-width: 200px; max-height: 150px; border-radius: 5px; border: 2px dashed #ddd; margin: 0 auto;">
          </div>
          <div style="text-align: center; margin-bottom: 15px;">
            <button type="button" id="upload-btn" style="padding: 8px 16px; background: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer;">
              更换图片
            </button>
          </div>
          <label class="edit-field"><span>商铺名称</span><input id="businessName" class="swal2-input" placeholder="例如：北洋食堂" value="${escapeHtml(business.value.businessName)}"></label>
          <label class="edit-field"><span>外送起送价</span><input id="startPrice" type="number" min="0" step="0.01" class="swal2-input" placeholder="自取不受此门槛限制" value="${escapeHtml(business.value.startPrice)}"></label>
          <label class="edit-field"><span>外送配送费</span><input id="deliveryPrice" type="number" min="0" step="0.01" class="swal2-input" placeholder="仅外送订单收取" value="${escapeHtml(business.value.deliveryPrice)}"></label>
          <label style="display:flex;align-items:center;gap:8px;width:90%;margin:8px auto;color:#45677d;font-size:13px;text-align:left;"><input id="dineInAvailable" type="checkbox" ${currentDineInAvailable ? 'checked' : ''}>支持堂食 <small style="margin-left:auto;color:#9aadb9;">首页显示“堂食店”</small></label>
          <label class="edit-field"><span>满减活动</span><select id="promotionPreset" class="swal2-input">
            <option value="" ${!currentPromotionValue ? 'selected' : ''}>不设置满减</option>
            <option value="20-3" ${currentPromotionValue === '20-3' ? 'selected' : ''}>满20减3</option>
            <option value="30-5" ${currentPromotionValue === '30-5' ? 'selected' : ''}>满30减5</option>
            <option value="50-10" ${currentPromotionValue === '50-10' ? 'selected' : ''}>满50减10</option>
          </select></label>
          <label class="edit-field"><span>商铺介绍</span><textarea id="businessExplain" class="swal2-textarea" placeholder="向顾客介绍你的店铺">${escapeHtml(business.value.businessExplain)}</textarea></label>
          <label class="edit-field"><span>经营类型</span><select id="orderTypeId" class="swal2-input" required>
            <option value="" disabled ${!currentOrderTypeId ? 'selected' : ''}>请选择商铺类型</option>
            <option value="1" ${currentOrderTypeId === 1 ? 'selected' : ''}>美食</option>
            <option value="2" ${currentOrderTypeId === 2 ? 'selected' : ''}>早餐</option>
            <option value="3" ${currentOrderTypeId === 3 ? 'selected' : ''}>跑腿代购</option>
            <option value="4" ${currentOrderTypeId === 4 ? 'selected' : ''}>汉堡披萨</option>
            <option value="5" ${currentOrderTypeId === 5 ? 'selected' : ''}>甜品饮品</option>
            <option value="6" ${currentOrderTypeId === 6 ? 'selected' : ''}>速食简食</option>
            <option value="7" ${currentOrderTypeId === 7 ? 'selected' : ''}>地方小吃</option>
            <option value="8" ${currentOrderTypeId === 8 ? 'selected' : ''}>米粉面馆</option>
            <option value="9" ${currentOrderTypeId === 9 ? 'selected' : ''}>包子粥铺</option>
            <option value="10" ${currentOrderTypeId === 10 ? 'selected' : ''}>炸鸡炸串</option>
          </select></label>
        `,
        focusConfirm: false,
        showCancelButton: true,
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        didOpen: () => {
          const fileInput = document.createElement('input');
          fileInput.type = 'file';
          fileInput.accept = 'image/*';
          fileInput.style.display = 'none';
          document.body.appendChild(fileInput);

          const uploadBtn = document.getElementById('upload-btn');
          const imagePreview = document.getElementById('image-preview');

          // 实时校验函数
          const validateField = (inputId, validationFn, errorMessage) => {
            const input = document.getElementById(inputId);
            if (input) {
              input.addEventListener('input', () => {
                const isValid = validationFn(input.value.trim());
                if (!isValid) {
                  input.style.borderColor = '#dc3545';
                  input.style.backgroundColor = '#fff5f5';
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

          // 商铺名称校验
          validateField('businessName', (value) => {
            return value.length > 0 && value.length <= 64;
          }, '商铺名称不能为空且不能超过64个字符');

          // 商铺介绍校验
          validateField('businessExplain', (value) => {
            return value.length <= 255;
          }, '商铺介绍不能超过255个字符');

          // 起送价校验
          validateField('startPrice', (value) => {
            if (!value) return false;
            const num = parseFloat(value);
            if (isNaN(num) || num < 0) return false;
            if (value.includes('.') && value.split('.')[1].length > 2) return false;
            return true;
          }, '起送价必须大于等于0，小数点最多保留两位');

          // 配送费校验
          validateField('deliveryPrice', (value) => {
            if (!value) return false;
            const num = parseFloat(value);
            if (isNaN(num) || num < 0) return false;
            if (value.includes('.') && value.split('.')[1].length > 2) return false;
            return true;
          }, '配送费必须大于等于0，小数点最多保留两位');

          uploadBtn.addEventListener('click', () => {
            fileInput.click();
          });

          fileInput.addEventListener('change', (event) => {
            if (event.target.files.length > 0) {
              selectedFile = event.target.files[0];
              const reader = new FileReader();
              reader.onload = (e) => {
                imagePreview.src = e.target.result;
              };
              reader.readAsDataURL(selectedFile);
            }
          });
        },
        preConfirm: async () => {
          const businessName = document.getElementById('businessName').value.trim();
          const startPrice = parseFloat(document.getElementById('startPrice').value);
          const deliveryPrice = parseFloat(document.getElementById('deliveryPrice').value);
          const businessExplain = document.getElementById('businessExplain').value.trim();
          const orderTypeId = document.getElementById('orderTypeId').value;
          const dineInAvailable = document.getElementById('dineInAvailable').checked;
          const promotionValue = document.getElementById('promotionPreset').value;

          if (!businessName || isNaN(startPrice) || isNaN(deliveryPrice) || !orderTypeId) {
            Swal.showValidationMessage('请填写完整且正确的信息');
            return false;
          }

          // 检查是否有字段校验错误
          const errorMessages = document.querySelectorAll('.field-error-message');
          if (errorMessages.length > 0) {
            Swal.showValidationMessage('请修正表单中的错误');
            return false;
          }

          let finalImageUrl = currentImageUrl;

          if (selectedFile) {
            try {
              const formData = new FormData();
              formData.append('file', selectedFile);

              Swal.showLoading();

              const uploadResponse = await request.post('/upload', formData, {
                headers: {
                  'Content-Type': 'multipart/form-data'
                }
              });

              if (uploadResponse && uploadResponse.data) {
                finalImageUrl = uploadResponse.data;
              } else {
                throw new Error('图片上传失败');
              }
            } catch (error) {
              Swal.showValidationMessage('图片上传失败，请重试');
              return false;
            }
          }

          return {
            businessName,
            businessImg: finalImageUrl,
            startPrice,
            deliveryPrice,
            businessExplain,
            orderTypeId: parseInt(orderTypeId),
            dineInAvailable,
            promotionThreshold: promotionValue ? parseFloat(promotionValue.split('-')[0]) : null,
            promotionDiscount: promotionValue ? parseFloat(promotionValue.split('-')[1]) : null
          };
        }
      });

      // 清理文件输入元素
      const fileInputs = document.querySelectorAll('input[type="file"]');
      fileInputs.forEach(input => {
        if (input.parentNode === document.body) {
          document.body.removeChild(input);
        }
      });

      if (formValues) {
        try {
          const updateData = {
            businessName: formValues.businessName,
            businessImg: formValues.businessImg,
            startPrice: formValues.startPrice,
            deliveryPrice: formValues.deliveryPrice,
            businessExplain: formValues.businessExplain,
            orderTypeId: formValues.orderTypeId,
            dineInAvailable: formValues.dineInAvailable,
            promotionThreshold: formValues.promotionThreshold,
            promotionDiscount: formValues.promotionDiscount
          };

          const response = await request.patch(`/api/businesses/own/${businessId.value}`, updateData);

          if (response.success) {
            business.value = {
              ...business.value,
              ...updateData
            };
console.log('修改成功，新的接口测试ok');
            Swal.fire({
              icon: 'success',
              title: '修改成功',
              text: '商铺信息已更新！',
              timer: 1500,
              showConfirmButton: false
            });
          } else {
            throw new Error(response.message || '修改失败');
          }
        } catch (error) {
          console.error('修改商铺信息失败:', error);
          Swal.fire('修改失败', error.response?.data?.message || error.message || '请稍后重试', 'error');
        }
      }
    };

    // 添加新商品
    const showAddNewFoodModal = async () => {
      let selectedFile = null;

      const { value: formValues } = await Swal.fire({
        title: '添加新商品',
        html: `
          <div style="text-align: center; margin-bottom: 15px;">
            <img id="image-preview" src="" style="max-width: 200px; max-height: 150px; border-radius: 5px; border: 2px dashed #ddd; display: none; margin: 0 auto;">
            <div id="no-image-text" style="color: #999; font-size: 14px;">暂无图片</div>
          </div>
          <div style="text-align: center; margin-bottom: 15px;">
            <button type="button" id="upload-btn" style="padding: 8px 16px; background: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer;">
              选择图片
            </button>
          </div>
          <label class="edit-field"><span>商品名称</span><input id="foodName" class="swal2-input" placeholder="例如：招牌牛肉面"></label>
          <label class="edit-field"><span>商品简介</span><input id="foodExplain" class="swal2-input" placeholder="例如：现点现做"></label>
          <label class="edit-field"><span>商品价格</span><input id="foodPrice" type="number" min="0" step="0.01" class="swal2-input" placeholder="请输入售价"></label>
          <label class="edit-field"><span>商品分类</span><input id="foodCategory" class="swal2-input" maxlength="32" list="food-category-options" placeholder="输入分类，例如：盖饭、饮品"><small>可以选择店内已有分类，也可以直接创建新分类</small></label>
          <datalist id="food-category-options">${categoryOptionsHtml()}</datalist>
          ${purchaseLimitFieldsHtml()}
        `,
        focusConfirm: false,
        showCancelButton: true,
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        didOpen: () => {
          bindPurchaseLimitFields();
          const fileInput = document.createElement('input');
          fileInput.type = 'file';
          fileInput.accept = 'image/*';
          fileInput.style.display = 'none';
          document.body.appendChild(fileInput);

          const uploadBtn = document.getElementById('upload-btn');
          const imagePreview = document.getElementById('image-preview');
          const noImageText = document.getElementById('no-image-text');

          // 实时校验函数
          const validateField = (inputId, validationFn, errorMessage) => {
            const input = document.getElementById(inputId);
            if (input) {
              input.addEventListener('input', () => {
                const isValid = validationFn(input.value.trim());
                if (!isValid) {
                  input.style.borderColor = '#dc3545';
                  input.style.backgroundColor = '#fff5f5';
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

          // 商品名称校验
          validateField('foodName', (value) => {
            return value.length > 0 && value.length <= 100;
          }, '商品名称不能为空且不能超过100个字符');

          // 商品简介校验
          validateField('foodExplain', (value) => {
            return value.length <= 255;
          }, '商品简介不能超过255个字符');

          // 商品价格校验
          validateField('foodPrice', (value) => {
            if (!value) return false;
            const num = parseFloat(value);
            if (isNaN(num) || num < 0) return false;
            if (value.includes('.') && value.split('.')[1].length > 2) return false;
            return true;
          }, '商品价格必须大于等于0，小数点最多保留两位');

          validateField('foodCategory', (value) => value.length > 0 && value.length <= 32,
            '商品分类不能为空且不能超过32个字符');

          uploadBtn.addEventListener('click', () => {
            fileInput.click();
          });

          fileInput.addEventListener('change', (event) => {
            if (event.target.files.length > 0) {
              selectedFile = event.target.files[0];
              const reader = new FileReader();
              reader.onload = (e) => {
                imagePreview.src = e.target.result;
                imagePreview.style.display = 'block';
                noImageText.style.display = 'none';
              };
              reader.readAsDataURL(selectedFile);
            }
          });
        },
        preConfirm: async () => {
          const foodName = document.getElementById('foodName').value.trim();
          const foodExplain = document.getElementById('foodExplain').value.trim();
          const foodPrice = parseFloat(document.getElementById('foodPrice').value);
          const category = document.getElementById('foodCategory').value.trim();
          const purchaseLimit = readPurchaseLimit();

          if (!foodName || !foodExplain || !category || category.length > 32 || isNaN(foodPrice) || foodPrice < 0 || (purchaseLimit !== null && (isNaN(purchaseLimit) || purchaseLimit < 1 || purchaseLimit > 999))) {
            Swal.showValidationMessage('请填写完整且正确的信息');
            return false;
          }

          // 检查是否有字段校验错误
          const errorMessages = document.querySelectorAll('.field-error-message');
          if (errorMessages.length > 0) {
            Swal.showValidationMessage('请修正表单中的错误');
            return false;
          }

          if (!selectedFile) {
            Swal.showValidationMessage('请选择商品图片');
            return false;
          }

          try {
            const formData = new FormData();
            formData.append('file', selectedFile);

            Swal.showLoading();

            const uploadResponse = await request.post('/upload', formData, {
              headers: {
                'Content-Type': 'multipart/form-data'
              }
            });

            if (uploadResponse && uploadResponse.data) {
              return {
                foodName,
                foodImg: uploadResponse.data,
                foodExplain,
                foodPrice, category, purchaseLimit
              };
            } else {
              throw new Error('上传失败');
            }
          } catch (error) {
            Swal.showValidationMessage('图片上传失败，请重试');
            return false;
          }
        }
      });

      // 清理文件输入元素
      const fileInputs = document.querySelectorAll('input[type="file"]');
      fileInputs.forEach(input => {
        if (input.parentNode === document.body) {
          document.body.removeChild(input);
        }
      });

      if (formValues) {
        try {
          const newFood = {
            foodName: formValues.foodName,
            foodImg: formValues.foodImg,
            foodExplain: formValues.foodExplain,
            foodPrice: formValues.foodPrice,
            businessId: businessId.value,
            stock: 100000,
            category: formValues.category,
            purchaseLimit: formValues.purchaseLimit,
            shelveStatus: 1
          };

          const response = await request.post('/api/foods/addItem', newFood);

          if (response.success) {
            await fetchBusinessFoodListData();
            Swal.fire({
              icon: 'success',
              title: '添加成功',
              text: '新商品已添加！',
              timer: 1500,
              showConfirmButton: false
            });
          } else {
            throw new Error(response.message || '添加失败');
          }
        } catch (error) {
          console.error('添加商品失败:', error);
          Swal.fire('添加失败', error.response?.data?.message || error.message || '请稍后重试', 'error');
        }
      }
    };

    // 编辑商品信息
    const showEditFoodModal = async (id, index) => {
      const foodItem = foodArr.value[index];
      let selectedFile = null;
      let currentImageUrl = foodItem.foodImg;

      const { value: formValues } = await Swal.fire({
        title: '编辑商品信息',
        html: `
          <div style="text-align: center; margin-bottom: 15px;">
            <img id="image-preview" src="${escapeHtml(foodItem.foodImg || foodDefaultImg)}" onerror="this.onerror=null;this.src='${escapeHtml(foodDefaultImg)}'" style="max-width: 200px; max-height: 150px; border-radius: 5px; border: 2px dashed #ddd; margin: 0 auto;">
          </div>
          <div style="text-align: center; margin-bottom: 15px;">
            <button type="button" id="upload-btn" style="padding: 8px 16px; background: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer;">
              更换图片
            </button>
          </div>
          <label class="edit-field"><span>商品名称</span><input id="foodName" class="swal2-input" placeholder="例如：招牌牛肉面" value="${escapeHtml(foodItem.foodName)}"></label>
          <label class="edit-field"><span>商品简介</span><input id="foodExplain" class="swal2-input" placeholder="例如：现点现做" value="${escapeHtml(foodItem.foodExplain)}"></label>
          <label class="edit-field"><span>商品价格</span><input id="foodPrice" type="number" min="0" step="0.01" class="swal2-input" placeholder="请输入售价" value="${escapeHtml(foodItem.foodPrice)}"></label>
          <label class="edit-field"><span>商品分类</span><input id="foodCategory" class="swal2-input" maxlength="32" list="food-category-options" placeholder="输入分类，例如：盖饭、饮品" value="${escapeHtml(foodItem.category || '其他')}"><small>可以选择店内已有分类，也可以直接创建新分类</small></label>
          <datalist id="food-category-options">${categoryOptionsHtml()}</datalist>
          ${purchaseLimitFieldsHtml(foodItem.purchaseLimit)}
        `,
        focusConfirm: false,
        showCancelButton: true,
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        didOpen: () => {
          bindPurchaseLimitFields();
          const fileInput = document.createElement('input');
          fileInput.type = 'file';
          fileInput.accept = 'image/*';
          fileInput.style.display = 'none';
          document.body.appendChild(fileInput);

          const uploadBtn = document.getElementById('upload-btn');
          const imagePreview = document.getElementById('image-preview');

          // 实时校验函数
          const validateField = (inputId, validationFn, errorMessage) => {
            const input = document.getElementById(inputId);
            if (input) {
              input.addEventListener('input', () => {
                const isValid = validationFn(input.value.trim());
                if (!isValid) {
                  input.style.borderColor = '#dc3545';
                  input.style.backgroundColor = '#fff5f5';
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

          // 商品名称校验
          validateField('foodName', (value) => {
            return value.length > 0 && value.length <= 100;
          }, '商品名称不能为空且不能超过100个字符');

          // 商品简介校验
          validateField('foodExplain', (value) => {
            return value.length <= 255;
          }, '商品简介不能超过255个字符');

          // 商品价格校验
          validateField('foodPrice', (value) => {
            if (!value) return false;
            const num = parseFloat(value);
            if (isNaN(num) || num < 0) return false;
            if (value.includes('.') && value.split('.')[1].length > 2) return false;
            return true;
          }, '商品价格必须大于等于0，小数点最多保留两位');

          validateField('foodCategory', (value) => value.length > 0 && value.length <= 32,
            '商品分类不能为空且不能超过32个字符');

          uploadBtn.addEventListener('click', () => {
            fileInput.click();
          });

          fileInput.addEventListener('change', (event) => {
            if (event.target.files.length > 0) {
              selectedFile = event.target.files[0];
              const reader = new FileReader();
              reader.onload = (e) => {
                imagePreview.src = e.target.result;
              };
              reader.readAsDataURL(selectedFile);
            }
          });
        },
        preConfirm: async () => {
          const foodName = document.getElementById('foodName').value.trim();
          const foodExplain = document.getElementById('foodExplain').value.trim();
          const foodPrice = parseFloat(document.getElementById('foodPrice').value);
          const category = document.getElementById('foodCategory').value.trim();
          const purchaseLimit = readPurchaseLimit();

          if (!foodName || !foodExplain || !category || category.length > 32 || isNaN(foodPrice) || foodPrice < 0 || (purchaseLimit !== null && (isNaN(purchaseLimit) || purchaseLimit < 1 || purchaseLimit > 999))) {
            Swal.showValidationMessage('请填写完整且正确的信息');
            return false;
          }

          // 检查是否有字段校验错误
          const errorMessages = document.querySelectorAll('.field-error-message');
          if (errorMessages.length > 0) {
            Swal.showValidationMessage('请修正表单中的错误');
            return false;
          }

          let finalImageUrl = currentImageUrl;

          if (selectedFile) {
            try {
              const formData = new FormData();
              formData.append('file', selectedFile);

              Swal.showLoading();

              const uploadResponse = await request.post('/upload', formData, {
                headers: {
                  'Content-Type': 'multipart/form-data'
                }
              });

              if (uploadResponse && uploadResponse.data) {
                finalImageUrl = uploadResponse.data;
              } else {
                throw new Error('图片上传失败');
              }
            } catch (error) {
              Swal.showValidationMessage('图片上传失败，请重试');
              return false;
            }
          }

          return {
            foodName,
            foodImg: finalImageUrl,
            foodExplain,
            foodPrice, category, purchaseLimit
          };
        }
      });

      // 清理文件输入元素
      const fileInputs = document.querySelectorAll('input[type="file"]');
      fileInputs.forEach(input => {
        if (input.parentNode === document.body) {
          document.body.removeChild(input);
        }
      });

      if (formValues) {
        try {
          const updateData = {
            foodId: id,
            foodName: formValues.foodName,
            foodImg: formValues.foodImg,
            foodExplain: formValues.foodExplain,
            foodPrice: formValues.foodPrice,
            businessId: businessId.value,
            category: formValues.category,
            purchaseLimit: formValues.purchaseLimit,
            purchaseLimitEnabled: formValues.purchaseLimit !== null
          };

          const response = await request.post('/api/foods/modifyItem', updateData);

          if (response.success) {
            foodArr.value[index] = {
              ...foodItem,
              foodName: formValues.foodName,
              foodImg: formValues.foodImg,
              foodExplain: formValues.foodExplain,
              foodPrice: formValues.foodPrice,
              category: formValues.category,
              purchaseLimit: formValues.purchaseLimit
            };

            Swal.fire({
              icon: 'success',
              title: '修改成功',
              text: '商品信息已更新！',
              timer: 1500,
              showConfirmButton: false
            });
          } else {
            throw new Error(response.message || '修改失败');
          }
        } catch (error) {
          console.error('修改商品失败:', error);
          Swal.fire('修改失败', error.response?.data?.message || error.message || '请稍后重试', 'error');
        }
      }
    };

    // 上架/下架商品
    const shelveFood = (id, shelveStatus, index) => {
      const newStatus = shelveStatus === 0 ? 1 : 0;
      request.get(`/api/foods/status?foodId=${id}&shelveStatus=${newStatus}`)
        .then((response) => {
          if (response.success) {
            foodArr.value[index].shelveStatus = newStatus;
            Swal.fire(shelveStatus == 0 ? '上架成功' : '下架成功');
          }
        });
    };

    // 删除商品
    const deleteFood = (id, index) => {
      Swal.fire({
        title: '确定要删除吗？',
        text: "删除后将无法恢复！",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#3085d6',
        cancelButtonColor: '#d33',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }).then((result) => {
        if (result.isConfirmed) {
          request.get(`/api/foods/delete?foodId=${id}`)
            .then((response) => {
              if (response.success) {
                foodArr.value.splice(index, 1);
                Swal.fire('删除成功');
              }
            });
        }
      });
    };

    const handleImageError = (event) => {
      const image = event?.target;
      if (!image || image.dataset.fallbackApplied === 'true') return;
      image.dataset.fallbackApplied = 'true';
      image.src = image.closest('.food')
        ? require('@/assets/food-default.png')
        : require('@/assets/business-default.png');
    };

    return {
      business,
      favoriteCount,
      foodArr,
      showEditBusinessModal,
      showAddNewFoodModal,
      showEditFoodModal,
      shelveFood,
      deleteFood,
      handleImageError,
      toggleOperatingStatus
    };
  }
};
</script>

<style scoped>
.food-category-note { color:#7890a0!important; font-size:12px!important; }
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

/****************** 总容器 ******************/
.wrapper {
  width: 100%;
  max-width: 720px;
  margin: 0 auto;
  min-height: 100vh;
  padding: 72px 0 88px;
  box-sizing: border-box;
  background-color: #f4f8fb;
}

/****************** header部分 ******************/
.wrapper header {
  width: min(100%, 720px);
  height: 56px;
  background-color: #0097ff;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  position: fixed;
  left: 50%;
  transform: translateX(-50%);
  top: 0;
  z-index: 1000;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px;
  box-sizing: border-box;
}

.wrapper header .fa-angle-left {
  font-size: 32px;
  cursor: pointer;
}

/****************** 商家信息卡片 ******************/
.business-info-card {
  padding: 18px;
  background-color: #fff;
  border: 1px solid #e4edf3;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  margin: 0 16px;
}

.business-info-card .business-logo {
  flex: 0 0 110px;
  width: 110px;
  height: 110px;
  border-radius: 8px;
  overflow: hidden;
  margin-right: 18px;
}

.business-info-card .business-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.business-info-card .info-details h1 {
  font-size: 24px;
  margin-bottom: 6px;
  color: #333;
}

.business-info-card .info-details .price-info {
  font-size: 14px;
  color: #666;
  margin-top: 6px;
}

.business-info-card .info-details .info-item {
  margin-right: 12px;
}

.business-info-card .info-details .explain-text {
  font-size: 14px;
  color: #888;
  margin-top: 10px;
  line-height: 1.5;
}

/****************** 点赞和收藏部分 ******************/
.likes-collections {
  display: flex;
  justify-content: space-around;
  background-color: #fff;
  margin: 14px 16px;
  padding: 14px 0;
  border: 1px solid #e4edf3;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  font-size: 14px;
  color: #666;
}

.likes-collections .icon-item {
  display: flex;
  align-items: center;
}

.likes-collections .icon-item .fa {
  margin-right: 7px;
  font-size: 18px;
  color: #0097ef;
}

/* 编辑商家按钮 */
.edit-button-container {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin: 0 16px 14px;
}

.edit-button,
.operating-button {
  flex: 1;
  min-height: 40px;
  background-color: #007bff;
  color: #fff;
  border: 1px solid #007bff;
  padding: 9px 14px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.3s;
}

.edit-button:hover {
  background-color: #0056b3;
}
.operating-button{background:#fff;color:#168bd1;border-color:#168bd1;cursor:pointer}.operating-button.closed{background:#168bd1;color:#fff}.sold-out-label{margin-left:6px;color:#8a98a4;font-size:11px}.store-settings .open-setting{border-color:#bfe2cc;background:#f1faf4;color:#34895a}.store-settings .closed-setting{border-color:#dce4e9;background:#f5f7f8;color:#7f8e99}

/****************** 食品列表部分 ******************/
.wrapper .food {
  width: auto;
  margin: 0 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.wrapper .food li {
  width: 100%;
  box-sizing: border-box;
  padding: 14px;
  user-select: none;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  border: 1px solid #e4edf3;
  border-radius: 10px;
}

.wrapper .food li .food-left {
  display: flex;
  align-items: center;
  min-width: 0;
}

.wrapper .food li .food-left img {
  flex: 0 0 80px;
  width: 80px;
  height: 80px;
  border-radius: 7px;
  object-fit: cover;
}

.wrapper .food li .food-left .food-left-info {
  min-width: 0;
  margin-left: 12px;
}

.wrapper .food li .food-left .food-left-info h3 {
  font-size: 16px;
  color: #555;
  margin-bottom: 5px;
}

.wrapper .food li .food-left .food-left-info p {
  font-size: 13px;
  color: #888;
  margin-top: 5px;
}

.wrapper .food li .food-left .food-left-info .food-price {
  font-size: 16px;
  color: #ff5722;
  font-weight: bold;
  margin-top: 8px;
}

.wrapper .food li .food-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.wrapper .food li .food-left .food-left-info .food-status {
  font-size: 12px;
  margin-bottom: 5px;
  color: #e41414;
}

.wrapper .food li .food-right .action-button {
  background-color: #0097ef;
  color: #fff;
  border: none;
  padding: 7px 11px;
  border-radius: 5px;
  font-size: 13px;
  cursor: pointer;
  margin-left: 7px;
  transition: background-color 0.3s;
}

.wrapper .food li .food-right .action-button:hover {
  background-color: #4492fc;
}

.wrapper .food li .food-right .shelve-button {
  background-color: #fed90b;
}

.wrapper .food li .food-right .shelve-button:hover {
  background-color: #fed90b !important;
  opacity: 0.9;
  transform: scale(1.05);
}

.wrapper .food li .food-right .delete-button {
  background-color: #b30200;
}


/* 底部添加商品按钮 */
.footer-button-container {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: min(100%, 720px);
  display: flex;
  justify-content: center;
  align-items: center;
  height: 72px;
  background-color: #fff;
  border-top: 1px solid #f0f0f0;
  z-index: 100;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
}

.add-food-button {
  width: calc(100% - 32px);
  background-color: #0097ef;
  color: #fff;
  min-height: 44px;
  padding: 10px 0;
  border-radius: 7px;
  font-size: 15px;
  font-weight: bold;
  border: none;
  cursor: pointer;
  transition: background-color 0.3s;
}

.add-food-button:hover {
  background-color: #007bb6;
}

.stock-label{font-size:12px;color:#7891a5;margin-left:6px}
.store-settings { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 7px; }
.store-settings span { padding: 3px 7px; border: 1px solid #cfe3f0; border-radius: 4px; background: #f5fbff; color: #168bd1; font-size: 11px; }
.store-settings .muted-setting { border-color: #e1e9ee; background: #fafcfd; color: #9aadb9; }

@media (max-width: 540px) {
  .wrapper { padding-top: 64px; }
  .business-info-card { align-items: flex-start; padding: 14px; }
  .business-info-card .business-logo { flex-basis: 82px; width: 82px; height: 82px; margin-right: 12px; }
  .business-info-card .info-details h1 { font-size: 19px; }
  .business-info-card .info-details .price-info,
  .business-info-card .info-details .explain-text { font-size: 12px; }
  .wrapper .food li { align-items: flex-start; gap: 10px; }
  .wrapper .food li .food-left img { flex-basis: 68px; width: 68px; height: 68px; }
  .wrapper .food li .food-right { flex-direction: column; gap: 5px; }
  .wrapper .food li .food-right .action-button { width: 54px; margin-left: 0; padding: 6px 8px; }
}
</style>
<style>
.swal2-html-container .edit-field{display:block;width:90%;margin:11px auto 0;text-align:left;color:#45677d;font-size:13px;font-weight:600}.swal2-html-container .edit-field>span{display:block;margin:0 0 5px}.swal2-html-container .edit-field>small{display:block;margin-top:5px;color:#91a4b1;font-size:11px;font-weight:400}.swal2-html-container .edit-field .swal2-input,.swal2-html-container .edit-field .swal2-textarea{width:100%;margin:0;height:42px;padding:8px 11px;border:1px solid #d8e5ee;border-radius:5px;box-sizing:border-box;font-size:14px}.swal2-html-container .edit-field .swal2-textarea{height:76px;resize:vertical}.swal2-html-container .edit-field .swal2-input:focus,.swal2-html-container .edit-field .swal2-textarea:focus{border-color:#58afe7;box-shadow:0 0 0 3px #e8f5fd;outline:0}
</style>
