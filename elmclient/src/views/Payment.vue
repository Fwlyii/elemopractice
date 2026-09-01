<template>
	<div class="back-btn-container">
    <BackButton style="margin-top: 2vw;"/>
  </div>
	<div class="wrapper">
		<!-- header部分 -->
		<header>
			<p>在线支付</p>
		</header>

		<!-- 加载中提示 -->
		<div v-if="loading" class="loading">
			<p>加载中...</p>
		</div>

		<template v-else>
			<div class="content">
				<!-- 订单信息部分 -->
				<div class="section order-section">
					<div class="section-header">
						<h3>订单基本信息</h3>
						<span class="total-amount">&#165;{{ orderDetail.orderTotal || '0.00' }}</span>
					</div>
					
					<!-- 配送信息 -->
					<div class="delivery-info">
						<div class="info-item">
							<i class="fa fa-map-marker"></i>
							<span>{{ orderDetail.address || '未选择地址' }}</span>
						</div>
						<div class="info-item">
							<i class="fa fa-user"></i>
							<span>{{ orderDetail.contactName }} {{ orderDetail.contactSex === 1 ? '先生' : '女士' }}</span>
						</div>
						<div class="info-item">
							<i class="fa fa-phone"></i>
							<span>{{ orderDetail.contactTel }}</span>
						</div>
					</div>

					<div class="section-header">
						<h3>订单详情</h3>
					</div>

					<!-- 商家信息和订单明细部分 -->
					<div class="merchant-details" v-show="isShowDetailet">
						<div class="merchant-info">
							<img :src="orderDetail.businessImg" :alt="orderDetail.businessName" class="merchant-logo">
							<div class="merchant-name">
								{{ orderDetail.businessName || '未知商家' }}
							</div>
						</div>

						<!-- 订单明细部分 -->
						<div class="order-details">
							<template v-if="orderDetail.foodList && orderDetail.foodList.length > 0">
								<div class="detail-item" v-for="item in orderDetail.foodList" :key="item.id">
									<span class="item-name">{{ item.foodName || '未知商品' }} &#165;{{ item.foodPrice }} &nbsp; × {{ item.quantity || 0 }}</span>
									<span class="item-price">&#165;{{ (item.foodPrice * item.quantity).toFixed(2) }}</span>
								</div>
							</template>
							<div class="detail-item delivery-fee">
								<span>配送费</span>
								<span>&#165;{{ orderDetail.deliveryPrice.toFixed(2) || '0.00' }}</span>
							</div>
						</div>
					</div>
				</div>

				<!-- 支付方式部分 -->
				<div class="section payment-section">
					<h3>选择支付方式</h3>
					<div class="payment-options">
						<div class="payment-option" :class="{ active: selectedPayment === 'alipay' }"
							@click="selectPayment('alipay')">
							<img src="../assets/alipay.png" alt="支付宝支付">
							<i class="fa fa-check-circle"></i>
						</div>
						<div class="payment-option" :class="{ active: selectedPayment === 'wechat' }"
							@click="selectPayment('wechat')">
							<img src="../assets/wechat.png" alt="微信支付">
							<i class="fa fa-check-circle"></i>
						</div>
					</div>
				</div>

				<!-- 支付按钮 -->
				<div class="payment-action">
					<button class="pay-button" @click="handlePayment">
						<span v-if="paying">支付中...</span>
						<span v-else>确认支付 &#165;{{ orderDetail?.orderTotal || '0.00' }}</span>
					</button>
				</div>
			</div>
		</template>

		<!-- 底部菜单部分 -->
	</div>
</template>
  
<script>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '../utils/request';
import { toast } from '../utils/toast';
import BackButton from '../components/BackButton.vue';

export default {
	name: 'Payment',
	components: {
		BackButton
	},
	setup() {
		const orderDetail = ref(null);
		const isShowDetailet = ref(true);
		const route = useRoute();
		const router = useRouter();
		const orderId = ref();
		const loading = ref(true);
		const selectedPayment = ref('alipay');
		const paying = ref(false);
		const deliveryPrice = ref(5); // 默认配送费，可根据实际情况调整

		// 获取订单详情
		const fetchOrderDetails = async () => {
			try {
				// 使用动态的orderId，而不是硬编码的24
				const response = await request.get("/api/orders/detail", {
					params: { orderId: orderId.value }
				});
				
				console.log("订单详情响应:", response);
				
				if (response.success) {
					// 正确的数据访问方式
					orderDetail.value = response.data;
				} else {
					console.error('获取订单详情失败:', response.data?.message);
					toast.error("获取订单信息失败，请重试！");
					router.push({ path: '/userAddress' });
				}
			} catch (error) {
				console.error('请求错误:', error);
				toast.error("获取订单信息失败，请重试！");
				router.push({ path: '/userAddress' });
			} finally {
				loading.value = false;
			}
		};

		// 支付处理
		const handlePayment = async () => {
			try {
				const response = await request.put("/api/orders/status?orderState=1&orderId=" + orderId.value);
				if (response.success) {
					// 支付成功，跳转到成功页面
					router.push({
						path: '/successfulPayment',
						query: { orderId: orderId.value }
					});
				} else {
					toast.error("支付失败" + response.data.message);
				}
			} catch (error) {
				console.error('支付失败:', error);
				toast.error("支付失败，请重试！");
			} finally {
				paying.value = false;
			}
		};

		const detailetShow = () => {
			isShowDetailet.value = !isShowDetailet.value;
		};

		const selectPayment = (type) => {
			selectedPayment.value = type;
		};

		onMounted(() => {
			orderId.value = route.query.orderId;
			console.log("获取到的orderId:", orderId.value);
			fetchOrderDetails();
		});

		return {
			orderId,
			orderDetail,
			isShowDetailet,
			detailetShow,
			handlePayment,
			loading,
			selectedPayment,
			selectPayment,
			paying,
			deliveryPrice,
		};
	}
}
</script>
  
<style scoped>
/****************** 总容器 ******************/
.wrapper {
	min-height: 100vh;
	background-color: #f5f7fa;
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

.content {
	padding-top: 14vw;
	padding-bottom: 32vw;
}

.section {
	background: white;
	border-radius: 3vw;
	margin: 3vw;
	padding: 4vw;
	box-shadow: 0 0.2vw 1vw rgba(0, 0, 0, 0.05);
}

.section-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 4vw;
}

.section-header h3 {
	font-size: 4.2vw;
	color: #333;
	font-weight: 500;
	margin: 0;
}

.total-amount {
	font-size: 5vw;
	color: #ff6b00;
	font-weight: bold;
}

/* 配送信息样式 */
.delivery-info {
	margin-bottom: 4vw;
	padding-bottom: 3vw;
	border-bottom: 1px solid #f0f0f0;
}

.info-item {
	display: flex;
	align-items: center;
	margin-bottom: 2vw;
	font-size: 3.6vw;
	color: #666;
}

.info-item i {
	margin-right: 2vw;
	color: #0097FF;
	width: 5vw;
	text-align: center;
}

.merchant-info {
	display: flex;
	align-items: center;
	padding: 3vw 0;
	cursor: pointer;
}

.merchant-logo {
	width: 12vw;
	height: 12vw;
	border-radius: 2vw;
	object-fit: cover;
	margin-right: 3vw;
}

.merchant-name {
	flex: 1;
	font-size: 4vw;
	color: #333;
	display: flex;
	align-items: center;
	gap: 2vw;
}

.fa-angle-down {
	transition: transform 0.3s ease;
}

.fa-angle-down.rotate {
	transform: rotate(180deg);
}

.order-details {
	margin-top: 3vw;
	padding-top: 3vw;
	border-top: 0.2vw solid #f5f7fa;
}

.detail-item {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 2vw 0;
	font-size: 3.6vw;
	color: #666;
}

.delivery-fee {
	border-top: 0.2vw dashed #eee;
	margin-top: 2vw;
	padding-top: 2vw;
	color: #333;
	font-weight: bold;
}

.payment-options {
	display: flex;
	gap: 3vw;
	margin-top: 4vw;
}

.payment-option {
	flex: 1;
	padding: 4vw;
	border: 0.2vw solid #eee;
	border-radius: 2vw;
	display: flex;
	align-items: center;
	justify-content: space-between;
	cursor: pointer;
	transition: all 0.3s ease;
	background: #f9f9f9;
}

.payment-option img {
	height: 8vw;
	width: auto;
	object-fit: contain;
}

.payment-option .fa-check-circle {
	font-size: 5vw;
	color: #ddd;
	transition: all 0.3s ease;
}

.payment-option.active {
	border-color: #38CA73;
	background: #f0fff5;
}

.payment-option.active .fa-check-circle {
	color: #38CA73;
}

.payment-action {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	padding: 4vw;
	background: white;
	box-shadow: 0 -0.2vw 1vw rgba(0, 0, 0, 0.05);
}

.pay-button {
	width: 100%;
	height: 12vw;
	border: none;
	border-radius: 6vw;
	background: linear-gradient(to right, #38CA73, #2EAF62);
	color: white;
	font-size: 4.2vw;
	font-weight: bold;
	display: flex;
	align-items: center;
	justify-content: center;
	cursor: pointer;
	transition: all 0.3s ease;
}

.pay-button:disabled {
	background: #ccc;
	cursor: not-allowed;
}

.pay-button:not(:disabled):active {
	transform: scale(0.98);
}

.loading {
	width: 100%;
	height: 100vh;
	display: flex;
	justify-content: center;
	align-items: center;
	font-size: 4vw;
	color: #666;
}
/* 1. 给 BackButton 父容器加固定定位，与 header 对齐 */
.back-btn-container {
  position: fixed; /* 固定定位，不随滚动移动 */
  left: 0vw; /* 距离左侧的距离，可根据需求调整 */
  top: 0vw; /* 距离顶部的距离，与 header 高度（12vw）适配，确保垂直居中 */
  z-index: 1001; /* 比 header 的 z-index:1000 高，避免被遮挡 */
}

/* 2. 样式穿透：确保 BackButton 内部图标/文字正常显示（可选，根据组件内部结构调整） */
::v-deep .back-button { /* 这里的 .back-button 是 BackButton 组件根元素的类名，需与组件内部一致 */
  width: 8vw; /* 调整按钮大小，按需修改 */
  height: 8vw;
  color: #fff; /* 按钮颜色，与 header 白色文字匹配 */
  /* 如果组件内部是图标，可加图标大小控制 */
}
</style>