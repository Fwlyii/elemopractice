<template>
	<div class="wrapper">
		<!-- header部分 -->
		<header>
			<p>购物车</p>
		</header>

		<!-- 购物车为空提示 -->
		<div class="empty-cart" v-if="cartItems.length === 0">
			<img src="../assets/empty-cart.png" alt="购物车为空">
			<p>您的购物车空空如也</p>
			<button @click="goBack">返回商家</button>
		</div>

		<!-- 购物车列表部分 -->
			<div v-else>
			<!-- 商家信息 -->
			<div class="business-info">
				<div class="business-info-title"><h3>{{ businessName }}</h3><button class="select-all" @click="toggleSelectAll">{{ allSelected ? '取消全选' : '全选' }}</button></div>
				<p class="selection-hint">已选 {{ selectedItems.length }} 份商品，可分批结算</p>
			</div>

			<ul class="cart">
				<li v-for="item in cartItems" :key="item.id">
					<label class="cart-select" :aria-label="`选择${item.foodName}`"><input type="checkbox" v-model="selectedFoodIds" :value="item.foodId"><span></span></label>
					<div class="cart-img">
						<!-- 这里假设您有食物图片的URL，如果没有可以移除或使用默认图片 -->
						<img :src="item.foodImg || require('../assets/food-default.png')" alt="食物图片" @error="handleImageError">
						<div class="cart-img-quantity" v-show="item.quantity > 0">{{ item.quantity }}</div>
					</div>
					<div class="cart-info">
						<h3>{{ item.foodName }}</h3>
						<small class="food-meta">{{ item.category || '招牌推荐' }}<span v-if="item.purchaseLimit"> · 每单限{{ item.purchaseLimit }}份</span></small>
						<p>&#165;{{ item.foodPrice }} / 份
							<span class="quantity-mark"> * {{ item.quantity }}</span>
						</p>
						<small class="stock-hint">剩余库存 {{ item.stock ?? 0 }}</small>
					</div>
					<div class="cart-item-price">
						<h3>￥ {{ item.foodPrice * item.quantity }}</h3>
					</div>
				</li>
			</ul>

			<!-- 底部结算栏 -->
			<div class="checkout-bar">
				<div class="total-price">
					<p style="color: black;">总计:  <span style="color:crimson;">&#165; {{ totalPrice }}</span></p>
				</div>
				<button class="checkout-btn" :disabled="selectedItems.length === 0" @click="checkout">去下单</button>
			</div>
		</div>

		<!-- 底部菜单部分 -->
		<!-- <Footer /> -->
	</div>
</template>

<script>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '../utils/request';
import { toast } from '../utils/toast';

export default {
	name: 'Cart',
	setup() {
		const cartItems = ref([]);
		const userInfo = ref(null);
		const route = useRoute();
		const router = useRouter();
		const businessId = ref(null);
		const selectedFoodIds = ref([]);
		// const businessId = ref(null);
		const businessName = ref('');

		onMounted(() => {
			businessId.value = parseInt(route.query.businessId);
			const cachedUser = localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo');
			try { userInfo.value = cachedUser ? JSON.parse(cachedUser) : null; } catch (_) { userInfo.value = null; }

			if (userInfo.value) {
				listCart();
			} else {
				toast.error("用户未登录，请先登录！");
				router.push({ path: '/login' });
			}
		});

		const listCart = () => {
			request.get("/api/carts/list?businessId=" + businessId.value)
				.then(response => {
					cartItems.value = Array.isArray(response?.data) ? response.data : [];
					businessName.value = cartItems.value[0]?.businessName || '当前商家';
					selectedFoodIds.value = [...new Set(cartItems.value.map(item => item.foodId).filter(Boolean))];
				}).catch(error => {
					console.error('获取购物车失败:', error);
			});
		};

		// 计算总价
		const selectedItems = computed(() => cartItems.value.filter(item => selectedFoodIds.value.includes(item.foodId)));
		const allSelected = computed(() => cartItems.value.length > 0 && selectedItems.value.length === cartItems.value.length);
		const totalPrice = computed(() => {
			return selectedItems.value.reduce((total, item) => {
				return total + (item.foodPrice * item.quantity);
			}, 0);
		});

		const toggleSelectAll = () => {
			selectedFoodIds.value = allSelected.value ? [] : [...new Set(cartItems.value.map(item => item.foodId).filter(Boolean))];
		};

		// 结算
		const checkout = () => {
			if (selectedItems.value.length === 0) {
				toast.warning('请先选择要结算的商品');
				return;
			}
			// 跳转到结算页面
				router.push({
					path: '/userAddress',
					query: {
						businessId: businessId.value,
						foodIds: selectedFoodIds.value.join(','),
						serviceMode: route.query.serviceMode || localStorage.getItem(`businessServiceMode:${businessId.value}`) || 'delivery',
					}
			});
		};

		// 返回商家页面
		const goBack = () => {
			router.go(-1);
		};

		const handleImageError = (event) => {
			const image = event?.target;
			if (!image || image.dataset.fallbackApplied === 'true') return;
			image.dataset.fallbackApplied = 'true';
			image.src = require('../assets/food-default.png');
		};

		return {
			cartItems,
			businessName,
			selectedFoodIds,
			selectedItems,
			allSelected,
			toggleSelectAll,
			totalPrice,
			checkout,
			goBack,
			handleImageError,
			businessId
		};
	}
}
</script>

<style scoped>
.stock-hint{display:block;color:#8aa0b2;font-size:12px;margin-top:4px}
.food-meta{display:block;color:#2384bd;font-size:12px;margin-top:4px}
.business-info-title{display:flex;align-items:center;justify-content:space-between;gap:12px}
.business-info-title h3{margin:0}
.selection-hint{margin:6px 0 0;color:#8aa0b2;font-size:12px}
.select-all{border:1px solid #a9d5ef;border-radius:14px;background:#f5fbff;color:#168bd1;padding:5px 10px;font-size:12px;cursor:pointer}
/****************** 总容器 ******************/
.wrapper {
	width: 100%;
	height: 100%;
	position: relative;
	top: -4vw;
}

/****************** header部分 ******************/
.wrapper header {
	width: 100%;
	height: 12vw;
	background-color: #0097FF;
	color: #fff;
	font-size: 4.8vw;
	position: fixed;
	left: 0;
	top: 0;
	z-index: 1000;
	display: flex;
	justify-content: center;
	align-items: center;
}

/****************** 商家信息 ******************/
.business-info {
	padding: 3vw;
	background-color: #f8f8f8;
	border-bottom: 1px solid #eee;
	margin-top: 12vw;
}

.business-info h3 {
	font-size: 4vw;
	color: #333;
}

/****************** 购物车列表部分 ******************/
.wrapper .cart {
	width: 100%;
	margin-bottom: 60px;
}

.wrapper .cart li {
	width: 100%;
	box-sizing: border-box;
	padding: 2.5vw;
	border-bottom: solid 1px #DDD;
	user-select: none;
	cursor: pointer;
	display: flex;
	align-items: center;
}
.cart-select{width:24px;flex:0 0 24px;display:flex;align-items:center;justify-content:center;margin-right:6px;cursor:pointer}
.cart-select input{position:absolute;opacity:0;pointer-events:none}
.cart-select span{width:18px;height:18px;border:1px solid #b5c9d8;border-radius:50%;background:#fff;position:relative}
.cart-select input:checked + span{border-color:#168bd1;background:#168bd1}
.cart-select input:checked + span::after{content:'✓';position:absolute;left:3px;top:-1px;color:#fff;font-size:14px;line-height:18px}

.wrapper .cart li .cart-img {
	/*这里设置为相当定位，成为cart-img-quantity元素的父元素*/
	position: relative;
}

.wrapper .cart li .cart-img img {
	width: 20vw;
	height: 20vw;
}

.wrapper .cart li .cart-img .cart-img-quantity {
	width: 5vw;
	height: 5vw;
	background-color: red;
	color: #fff;
	font-size: 3.6vw;
	border-radius: 2.5vw;
	display: flex;
	justify-content: center;
	align-items: center;
	/*设置成绝对定位，不占文档流空间*/
	position: absolute;
	right: -1.5vw;
	top: -1.5vw;
}

.wrapper .cart li .cart-info {
	margin-left: 3vw;
	flex: 1;
}

.wrapper .cart li .cart-info h3 {
	font-size: 3.8vw;
	color: #555;
}

.wrapper .cart li .cart-info p {
	font-size: 3vw;
	color: #888;
	margin-top: 2vw;
}

.wrapper .cart li .cart-actions {
	display: flex;
	align-items: center;
	gap: 2vw;
}

.wrapper .cart li .cart-actions button {
	width: 6vw;
	height: 6vw;
	border: none;
	border-radius: 50%;
	background-color: #0097FF;
	color: white;
	font-size: 3.5vw;
	display: flex;
	justify-content: center;
	align-items: center;
	cursor: pointer;
}

.wrapper .cart li .cart-actions .delete-btn {
	width: auto;
	padding: 0 2vw;
	border-radius: 1vw;
	font-size: 2.8vw;
	background-color: #ff4d4f;
	margin-left: 2vw;
}

.wrapper .cart li .cart-actions span {
	font-size: 3.5vw;
	color: #555;
}

/****************** 结算栏 ******************/
.checkout-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	width: 100%;
	height: 14vw;
	background-color: #fff;
	border-top: 1px solid #ddd;
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 0 4vw;
	box-sizing: border-box;
}

.checkout-bar .total-price {
	font-size: 4.5vw;
	font-weight: bold;
	color: #ff4d4f;
}

.checkout-bar .checkout-btn {
	background-color: #0097FF;
	color: white;
	border: none;
	padding: 2.5vw 5vw;
	border-radius: 2vw;
	font-size: 4vw;
	cursor: pointer;
}
.checkout-bar .checkout-btn:disabled{background:#b8cbd7;cursor:not-allowed;box-shadow:none}

/****************** 空购物车 ******************/
.empty-cart {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	padding-top: 30vw;
}

.empty-cart img {
	width: 40vw;
	height: 40vw;
	margin-bottom: 5vw;
}

.empty-cart p {
	font-size: 4vw;
	color: #999;
	margin-bottom: 5vw;
}

.empty-cart button {
	background-color: #0097FF;
	color: white;
	border: none;
	padding: 3vw 6vw;
	border-radius: 2vw;
	font-size: 4vw;
	cursor: pointer;
}
.back-btn-container {
  position: fixed; /* 固定定位，不随滚动移动 */
  left: 0vw; /* 距离左侧的距离，可根据需求调整 */
  top: 1vw; /* 距离顶部的距离，与 header 高度（12vw）适配，确保垂直居中 */
  z-index: 1001; /* 比 header 的 z-index:1000 高，避免被遮挡 */
}

/* 桌面端也保持移动端外卖页面的窄栏比例，避免 vw 字号随窗口放大造成拥挤。 */
.wrapper{width:100%;max-width:600px;min-height:100vh;height:auto;margin:0 auto;top:0;background:#f7fafc;color:#29455f;overflow-x:hidden;box-sizing:border-box}
.wrapper header{width:100%;height:56px;position:fixed;left:50%;top:0;transform:translateX(-50%);max-width:600px;font-size:20px;z-index:1000}
.business-info{margin-top:56px;padding:14px 16px;background:#fff;border-bottom:1px solid #e5edf2}
.business-info h3{font-size:18px;color:#31556d}
.business-info-title h3{min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.selection-hint{font-size:12px}
.wrapper .cart{margin:0 0 82px;padding:0;list-style:none;background:#f7fafc}
.wrapper .cart li{padding:14px 12px;gap:8px;align-items:center;background:#fff;border-bottom:1px solid #e7eef3;min-height:104px}
.wrapper .cart li .cart-img img{width:76px;height:76px;object-fit:cover;border-radius:6px}
.wrapper .cart li .cart-img .cart-img-quantity{width:22px;height:22px;right:-7px;top:-7px;border-radius:50%;font-size:12px}
.wrapper .cart li .cart-info{margin-left:4px;min-width:0}
.wrapper .cart li .cart-info h3{font-size:15px;color:#31556d;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.wrapper .cart li .cart-info p{font-size:13px;color:#708797;margin-top:7px}
.wrapper .cart li .cart-info .quantity-mark{font-size:13px;color:#b33e48}
.wrapper .cart li .cart-info .stock-hint{white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.wrapper .cart li .cart-item-price{margin-left:auto;white-space:nowrap}
.wrapper .cart li .cart-item-price h3{font-size:14px;color:#31556d}
.checkout-bar{left:50%;transform:translateX(-50%);max-width:600px;width:100%;height:64px;padding:0 14px;background:#fff;border-top:1px solid #dfeaf1;box-shadow:0 -2px 8px rgba(40,84,110,.08)}
.checkout-bar .total-price{font-size:15px;color:#31556d}
.checkout-bar .total-price p{font-size:15px!important}
.checkout-bar .checkout-btn{padding:10px 24px;border-radius:6px;font-size:15px;background:#168bd1}
.empty-cart{padding-top:120px}
.empty-cart img{width:160px;height:160px;margin-bottom:18px}
.empty-cart p{font-size:15px;margin-bottom:18px}
.empty-cart button{padding:10px 22px;border-radius:6px;font-size:14px}
.back-btn-container{left:max(12px, calc(50% - 288px));top:8px}
:deep(.back-button){left:max(16px, calc(50% - 284px));top:10px}
@media (max-width:600px){.wrapper header,.checkout-bar{left:0;transform:none}.back-btn-container{left:8px}}
@media (max-width:600px){:deep(.back-button){left:16px}}
</style>
