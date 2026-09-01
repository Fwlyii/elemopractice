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
        <img :src="business.businessImg || require('@/assets/default-business.png')" />
      </div>
      <div class="info-details">
        <h1>{{ business.businessName }}</h1>
        <p class="price-info">
          <span class="info-item">起送价 &#165;{{ business.startPrice }}</span>
          <span class="info-item">配送费 &#165;{{ business.deliveryPrice }}</span>
        </p>
        <p class="explain-text">{{ business.businessExplain }}</p>
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
      <button class="edit-button" @click="showEditBusinessModal">编辑商家信息</button>
    </div>

    <ul class="food">
      <li v-for="(item, index) in foodArr" :key="item.foodId">
        <div class="food-left">
          <img :src="item.foodImg" />
          <div class="food-left-info">
            <h3>{{ item.foodName }}</h3>
            <text class="food-status" v-if="item.shelveStatus === 0">(已下架)</text>
            <p>{{ item.foodExplain }}</p>
            <p class="food-price">&#165;{{ item.foodPrice }}</p>
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

export default {
  name: "BusinessInfo",
  setup() {
    const route = useRoute();
    const businessId = ref();
    const business = ref({});
    const foodArr = ref([]);
    const favoriteCount = ref({});

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

      const { value: formValues } = await Swal.fire({
        title: '编辑商铺信息',
        html: `
          <div style="text-align: center; margin-bottom: 15px;">
            <img id="image-preview" src="${business.value.businessImg}" style="max-width: 200px; max-height: 150px; border-radius: 5px; border: 2px dashed #ddd; margin: 0 auto;">
          </div>
          <div style="text-align: center; margin-bottom: 15px;">
            <button type="button" id="upload-btn" style="padding: 8px 16px; background: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer;">
              更换图片
            </button>
          </div>
          <input id="businessName" class="swal2-input" placeholder="商铺名称" value="${business.value.businessName}">
          <input id="startPrice" type="number" class="swal2-input" placeholder="起送费" value="${business.value.startPrice}">
          <input id="deliveryPrice" type="number" class="swal2-input" placeholder="配送费" value="${business.value.deliveryPrice}">
          <textarea id="businessExplain" class="swal2-textarea" placeholder="商铺介绍">${business.value.businessExplain}</textarea>
          <select id="orderTypeId" class="swal2-input" required style="width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px;">
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
          </select>
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

          // 商铺名称校验（最多10个字符）
          validateField('businessName', (value) => {
            return value.length <= 10;
          }, '商铺名称不能超过10个字符');

          // 商铺介绍校验（最多15个字符）
          validateField('businessExplain', (value) => {
            return value.length <= 15;
          }, '商铺介绍不能超过15个字符');

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
            orderTypeId: parseInt(orderTypeId)
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
            orderTypeId: formValues.orderTypeId
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
          <input id="foodName" class="swal2-input" placeholder="商品名称">
          <input id="foodExplain" class="swal2-input" placeholder="商品简介">
          <input id="foodPrice" type="number" class="swal2-input" placeholder="商品价格">
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

          // 商品名称校验（最多6个字符）
          validateField('foodName', (value) => {
            return value.length <= 6;
          }, '商品名称不能超过6个字符');

          // 商品简介校验（最多8个字符）
          validateField('foodExplain', (value) => {
            return value.length <= 8;
          }, '商品简介不能超过8个字符');

          // 商品价格校验
          validateField('foodPrice', (value) => {
            if (!value) return false;
            const num = parseFloat(value);
            if (isNaN(num) || num < 0) return false;
            if (value.includes('.') && value.split('.')[1].length > 2) return false;
            return true;
          }, '商品价格必须大于等于0，小数点最多保留两位');

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

          if (!foodName || !foodExplain || isNaN(foodPrice)) {
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
                foodPrice
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
            <img id="image-preview" src="${foodItem.foodImg}" style="max-width: 200px; max-height: 150px; border-radius: 5px; border: 2px dashed #ddd; margin: 0 auto;">
          </div>
          <div style="text-align: center; margin-bottom: 15px;">
            <button type="button" id="upload-btn" style="padding: 8px 16px; background: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer;">
              更换图片
            </button>
          </div>
          <input id="foodName" class="swal2-input" placeholder="商品名称" value="${foodItem.foodName}">
          <input id="foodExplain" class="swal2-input" placeholder="商品简介" value="${foodItem.foodExplain}">
          <input id="foodPrice" type="number" class="swal2-input" placeholder="商品价格" value="${foodItem.foodPrice}">
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

          // 商品名称校验（最多6个字符）
          validateField('foodName', (value) => {
            return value.length <= 6;
          }, '商品名称不能超过6个字符');

          // 商品简介校验（最多8个字符）
          validateField('foodExplain', (value) => {
            return value.length <= 8;
          }, '商品简介不能超过8个字符');

          // 商品价格校验
          validateField('foodPrice', (value) => {
            if (!value) return false;
            const num = parseFloat(value);
            if (isNaN(num) || num < 0) return false;
            if (value.includes('.') && value.split('.')[1].length > 2) return false;
            return true;
          }, '商品价格必须大于等于0，小数点最多保留两位');

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

          if (!foodName || !foodExplain || isNaN(foodPrice)) {
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
            foodPrice
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
            businessId: businessId.value
          };

          const response = await request.post('/api/foods/modifyItem', updateData);

          if (response.success) {
            foodArr.value[index] = {
              ...foodItem,
              foodName: formValues.foodName,
              foodImg: formValues.foodImg,
              foodExplain: formValues.foodExplain,
              foodPrice: formValues.foodPrice
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

    return {
      business,
      favoriteCount,
      foodArr,
      showEditBusinessModal,
      showAddNewFoodModal,
      showEditFoodModal,
      shelveFood,
      deleteFood
    };
  }
};
</script>

<style scoped>
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
  min-height: 100vh;
  padding-bottom: 14vw;
  box-sizing: border-box;
  background-color: #f5f5f5;
}

/****************** header部分 ******************/
.wrapper header {
  width: 100%;
  height: 12vw;
  background-color: #0097ff;
  color: #fff;
  font-size: 4.8vw;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 1000;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 4vw;
  box-sizing: border-box;
}

.wrapper header .fa-angle-left {
  font-size: 7vw;
  cursor: pointer;
}

/****************** 商家信息卡片 ******************/
.business-info-card {
  margin-top: 15vw;
  padding: 4vw;
  background-color: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  margin: 15vw 4vw 0;
}

.business-info-card .business-logo {
  width: 25vw;
  height: 25vw;
  border-radius: 5px;
  overflow: hidden;
  margin-right: 4vw;
}

.business-info-card .business-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.business-info-card .info-details h1 {
  font-size: 5vw;
  margin-bottom: 1vw;
  color: #333;
}

.business-info-card .info-details .price-info {
  font-size: 3vw;
  color: #666;
  margin-top: 1vw;
}

.business-info-card .info-details .info-item {
  margin-right: 2vw;
}

.business-info-card .info-details .explain-text {
  font-size: 3.2vw;
  color: #888;
  margin-top: 2vw;
  line-height: 1.5;
}

/****************** 点赞和收藏部分 ******************/
.likes-collections {
  display: flex;
  justify-content: space-around;
  background-color: #fff;
  margin: 3vw 4vw;
  padding: 3vw 0;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  font-size: 3.5vw;
  color: #666;
}

.likes-collections .icon-item {
  display: flex;
  align-items: center;
}

.likes-collections .icon-item .fa {
  margin-right: 1.5vw;
  font-size: 4.5vw;
  color: #0097ef;
}

/* 编辑商家按钮 */
.edit-button-container {
  display: flex;
  justify-content: center;
  margin-top: 4vw;
}

.edit-button {
  background-color: #007bff;
  color: #fff;
  border: none;
  padding: 2.5vw 5vw;
  border-radius: 5px;
  font-size: 3.5vw;
  cursor: pointer;
  transition: background-color 0.3s;
}

.edit-button:hover {
  background-color: #0056b3;
}

/****************** 食品列表部分 ******************/
.wrapper .food {
  width: 100%;
  margin-bottom: 14vw;
  margin-top: 4vw;
}

.wrapper .food li {
  width: 100%;
  box-sizing: border-box;
  padding: 4vw;
  user-select: none;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  border-bottom: 1px solid #eee;
}

.wrapper .food li .food-left {
  display: flex;
  align-items: center;
}

.wrapper .food li .food-left img {
  width: 18vw;
  height: 18vw;
  border-radius: 5px;
  object-fit: cover;
}

.wrapper .food li .food-left .food-left-info {
  margin-left: 3vw;
}

.wrapper .food li .food-left .food-left-info h3 {
  font-size: 4vw;
  color: #555;
  margin-bottom: 1vw;
}

.wrapper .food li .food-left .food-left-info p {
  font-size: 3vw;
  color: #888;
  margin-top: 1vw;
}

.wrapper .food li .food-left .food-left-info .food-price {
  font-size: 3.8vw;
  color: #ff5722;
  font-weight: bold;
  margin-top: 2vw;
}

.wrapper .food li .food-right {
  display: flex;
  align-items: center;
}

.wrapper .food li .food-left .food-left-info .food-status {
  font-size: 4vw;
  margin-bottom: 1vw;
  color: #e41414;
}

.wrapper .food li .food-right .action-button {
  background-color: #0097ef;
  color: #fff;
  border: none;
  padding: 2vw 3.5vw;
  border-radius: 5px;
  font-size: 3vw;
  cursor: pointer;
  margin-left: 2vw;
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
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 14vw;
  background-color: #fff;
  border-top: 1px solid #f0f0f0;
  z-index: 100;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
}

.add-food-button {
  width: 90%;
  background-color: #0097ef;
  color: #fff;
  padding: 3.5vw 0;
  border-radius: 8vw;
  font-size: 4.5vw;
  font-weight: bold;
  border: none;
  cursor: pointer;
  transition: background-color 0.3s;
}

.add-food-button:hover {
  background-color: #007bb6;
}
</style>