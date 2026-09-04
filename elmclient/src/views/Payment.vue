<template>
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
			<div v-if="!canPay" class="payment-unavailable">
				<i class="fa fa-check-circle"></i>
				<h2>{{ orderDetail.orderState === ORDER_STATUS.CANCELLED ? '订单已取消' : '该订单无需再次支付' }}</h2>
				<p>订单状态已经发生变化，请前往订单详情查看最新进度。</p>
				<button type="button" @click="router.push({ path: '/listDetail', query: { orderId } })">查看订单详情</button>
			</div>
			<div v-else class="content">
				<!-- 订单信息部分 -->
				<div class="section order-section">
					<div class="section-header">
						<h3>订单基本信息</h3>
						<span class="total-amount">&#165;{{ orderDetail.orderTotal || '0.00' }}</span>
					</div>
					
					<!-- 配送信息 -->
					<div v-if="orderDetail.serviceMode !== 'PICKUP'" class="delivery-info">
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
					<div v-else class="pickup-payment-note"><i class="fa fa-shopping-bag"></i><span><strong>到店自取</strong><small>商家备餐完成后到店取餐，无需配送地址</small></span></div>

					<div class="section-header">
						<h3>订单详情</h3>
					</div>

					<!-- 商家信息和订单明细部分 -->
					<div class="merchant-details" v-show="isShowDetailet">
						<div class="merchant-info">
							<img :src="orderDetail.businessImg || require('../assets/business-default.png')" :alt="orderDetail.businessName || '商家图片'" class="merchant-logo" @error="handleImageError">
							<div class="merchant-name">
								{{ orderDetail.businessName || '未知商家' }}
							</div>
						</div>

						<!-- 订单明细部分 -->
						<div class="order-details">
							<template v-if="orderDetail.foodList && orderDetail.foodList.length > 0">
								<div class="detail-item" v-for="item in orderDetail.foodList" :key="item.id">
									<span class="item-name">{{ item.foodName || '未知商品' }} &#165;{{ item.foodPrice }} &nbsp; × {{ item.quantity || 0 }}</span>
									<span class="item-price">&#165;{{ (Number(item.foodPrice || 0) * Number(item.quantity || 0)).toFixed(2) }}</span>
								</div>
							</template>
							<div v-if="merchantDiscount > 0" class="detail-item coupon-discount">
								<span>商家及会员优惠</span>
								<span>-&#165;{{ merchantDiscount.toFixed(2) }}</span>
							</div>
							<div v-if="orderDetail.serviceMode !== 'PICKUP'" class="detail-item delivery-fee">
								<span>配送费</span>
								<span>&#165;{{ Number(orderDetail.deliveryPrice || 0).toFixed(2) }}</span>
							</div>
							<div v-if="couponDiscount > 0" class="detail-item coupon-discount">
								<span>{{ selectedCoupon.name || '红包优惠' }}</span>
								<span>-&#165;{{ couponDiscount.toFixed(2) }}</span>
							</div>
						</div>
					</div>

					<div class="coupon-panel" aria-label="红包优惠">
						<div class="coupon-panel-header"><h3>红包</h3><span v-if="couponDiscount > 0">已减 ¥{{ couponDiscount.toFixed(2) }}</span></div>
						<div v-if="couponLoading" class="coupon-empty">正在加载可用红包…</div>
						<template v-else-if="usableCoupons.length">
							<button type="button" class="coupon-option" :class="{ active: selectedCouponId === null }" @click="selectCoupon(null)">
								<span><b>不使用红包</b><small>保留本次红包</small></span><i v-if="selectedCouponId === null" class="fa fa-check-circle"></i>
							</button>
							<button v-for="coupon in usableCoupons" :key="coupon.id" type="button" class="coupon-option" :class="{ active: selectedCouponId === coupon.id }" @click="selectCoupon(coupon.id)">
								<span><b>{{ coupon.name || '红包' }} · 减 ¥{{ Number(coupon.discountAmount || 0).toFixed(2) }}</b><small>满 ¥{{ Number(coupon.minOrderAmount || 0).toFixed(2) }} 可用 · {{ formatCouponExpiry(coupon.expiresAt) }}到期</small></span><i v-if="selectedCouponId === coupon.id" class="fa fa-check-circle"></i>
							</button>
						</template>
						<div v-else class="coupon-empty">暂无满足本单门槛的红包</div>
					</div>
				</div>

				<!-- 支付方式部分 -->
				<div class="section payment-section">
					<h3>选择支付方式</h3>
					<div class="payment-options">
						<div class="payment-option" :class="{ active: selectedPayment === 'alipay' }"
							@click="selectPayment('alipay')">
							<img src="../assets/alipay.png" alt="支付宝支付">
							<span class="payment-demo-badge">演示</span>
							<i class="fa fa-check-circle"></i>
						</div>
						<div class="payment-option" :class="{ active: selectedPayment === 'wechat' }"
							@click="selectPayment('wechat')">
							<img src="../assets/wechat.png" alt="微信支付">
							<span class="payment-demo-badge">演示</span>
							<i class="fa fa-check-circle"></i>
						</div>
						<div class="payment-option wallet-option" :class="{ active: selectedPayment === 'wallet' }"
							@click="selectPayment('wallet')">
							<span class="wallet-icon">¥</span>
							<strong>钱包余额</strong>
							<i class="fa fa-check-circle"></i>
						</div>
					</div>
					<p class="payment-demo-note">支付宝、微信支付仅模拟支付结果，不连接真实资金渠道。</p>
					<div v-if="assetInfo" class="asset-pay-hint">
						<span>钱包余额 ¥{{ Number(assetInfo.balance || 0).toFixed(2) }} · 可用积分 {{ assetInfo.points || 0 }}</span>
						<label v-if="maxPoints > 0">积分抵扣
							<input v-model.number="pointsToUse" type="number" min="0" step="100" :max="maxPoints" @input="normalizePoints">
						</label>
						<small v-if="maxPoints > 0">本单最多可抵 {{ maxPoints }} 积分（应付金额的20%）</small>
					</div>
				</div>

				<!-- 支付按钮 -->
				<div class="payment-action">
					<button class="pay-button" @click="handlePayment">
						<span v-if="paying">支付中...</span>
						<span v-else>确认支付 &#165;{{ payableAmount }}</span>
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
import { ORDER_STATUS } from '../utils/orderPresentation';

export default {
	name: 'Payment',
	setup() {
		const orderDetail = ref({});
		const isShowDetailet = ref(true);
		const route = useRoute();
		const router = useRouter();
		const orderId = ref();
		const loading = ref(true);
		const selectedPayment = ref('alipay');
		const paying = ref(false);
		const assetInfo = ref(null);
		const pointsToUse = ref(0);
		const availableCoupons = ref([]);
		const selectedCouponId = ref(null);
		const couponLoading = ref(false);
		const canPay = computed(() => Number(orderDetail.value?.orderState) === ORDER_STATUS.WAITING_PAYMENT);
		const foodSubtotal = computed(() => (orderDetail.value?.foodList || []).reduce((sum, item) => sum
			+ Number(item.foodPrice || 0) * Number(item.quantity || 0), 0));
		const merchantDiscount = computed(() => Math.max(0, foodSubtotal.value
			+ Number(orderDetail.value?.deliveryPrice || 0) - Number(orderDetail.value?.orderTotal || 0)));
		const couponBaseAmount = computed(() => Math.max(0, Number(orderDetail.value?.orderTotal || 0) - Number(orderDetail.value?.deliveryPrice || 0)));
		const usableCoupons = computed(() => availableCoupons.value
			.filter(coupon => Number(coupon.minOrderAmount || 0) <= couponBaseAmount.value)
			.sort((a, b) => Number(b.discountAmount || 0) - Number(a.discountAmount || 0)));
		const selectedCoupon = computed(() => usableCoupons.value.find(coupon => coupon.id === selectedCouponId.value) || {});
		const couponDiscount = computed(() => {
			const discount = Number(selectedCoupon.value.discountAmount || 0);
			return Math.min(Math.max(0, discount), couponBaseAmount.value);
		});
		const maxPoints = computed(() => {
			const total = Math.max(0, Number(orderDetail.value?.orderTotal || 0) - couponDiscount.value);
			const orderCap = Math.floor(total * 0.2 * 100);
			const available = Math.max(0, Number(assetInfo.value?.points || 0));
			return Math.floor(Math.min(orderCap, available) / 100) * 100;
		});
		const payableAmount = computed(() => (Math.max(0, Number(orderDetail.value?.orderTotal || 0) - couponDiscount.value - Number(pointsToUse.value || 0) / 100)).toFixed(2));

		// 获取订单详情
		const fetchOrderDetails = async () => {
			try {
				// 使用动态的orderId，而不是硬编码的24
				const response = await request.get("/api/orders/detail", {
					params: { orderId: orderId.value }
				});
				
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
		const fetchCoupons = async () => {
			couponLoading.value = true;
			try {
				const response = await request.get('/api/v1/assets/coupons');
				availableCoupons.value = response?.success && Array.isArray(response.data) ? response.data : [];
				// 默认勾选优惠力度最大的可用红包，用户仍可手动取消或改选。
				selectedCouponId.value = usableCoupons.value[0]?.id ?? null;
			} catch (error) {
				availableCoupons.value = [];
				selectedCouponId.value = null;
			} finally {
				couponLoading.value = false;
			}
		};
		const selectCoupon = (couponId) => {
			selectedCouponId.value = couponId;
			pointsToUse.value = Math.min(pointsToUse.value, maxPoints.value);
		};
		const formatCouponExpiry = (value) => value ? new Date(value).toLocaleDateString('zh-CN') : '近期';

		// 支付处理
		const handlePayment = async () => {
			if (!canPay.value) {
				toast.warning('订单状态已变化，无需再次支付');
				return;
			}
			if (paying.value) return;
			paying.value = true;
			try {
				const response = await request.put('/api/orders/status', null, { params: {
					orderState: ORDER_STATUS.WAITING_MERCHANT_ACCEPT,
					orderId: orderId.value,
					paymentMethod: selectedPayment.value === 'wallet' ? 'wallet' : 'simulated',
					pointsToUse: Number(pointsToUse.value || 0),
					couponId: selectedCouponId.value || undefined
				} });
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
		const normalizePoints = () => {
			const available = Number(assetInfo.value?.points || 0);
			pointsToUse.value = Math.min(maxPoints.value, available, Math.max(0, Math.floor(Number(pointsToUse.value || 0) / 100) * 100));
		};
		const handleImageError = (event) => {
			// 远程图片失效时回退到随前端一起部署的本地占位图，避免只显示 alt 文本。
			if (event.target.dataset.fallbackApplied) return;
			event.target.dataset.fallbackApplied = 'true';
			event.target.src = require('../assets/business-default.png');
		};

			onMounted(() => {
			orderId.value = route.query.orderId;
				fetchOrderDetails().then(() => { if (canPay.value) fetchCoupons(); });
			request.get('/api/v1/assets/me').then(response => {
				if (response?.success) assetInfo.value = response.data;
			}).catch(() => { /* 未登录或资产接口不可用时仍可使用模拟支付 */ });
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
			canPay,
			router,
			assetInfo,
			pointsToUse,
			maxPoints,
			payableAmount,
			normalizePoints,
			availableCoupons,
			usableCoupons,
			selectedCouponId,
			selectedCoupon,
			couponDiscount,
			merchantDiscount,
			couponLoading,
			selectCoupon,
			formatCouponExpiry,
			handleImageError,
			ORDER_STATUS,
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
.payment-unavailable{max-width:520px;margin:100px auto 0;padding:48px 24px;text-align:center;color:#5d7284}
.payment-unavailable>i{font-size:46px;color:#71add4}.payment-unavailable h2{margin:16px 0 8px;color:#29455f;font-size:20px}.payment-unavailable p{font-size:13px;line-height:1.7}.payment-unavailable button{margin-top:22px;border:0;border-radius:7px;background:#168bd1;color:#fff;padding:10px 22px;font-size:14px;cursor:pointer}

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
.pickup-payment-note { display:flex; align-items:center; gap:12px; padding:12px 14px; border:1px solid #d9ecf8; border-radius:10px; background:#f5fbff; color:#168bd1; }
.pickup-payment-note i { font-size:22px; }.pickup-payment-note strong,.pickup-payment-note small { display:block; }.pickup-payment-note small { margin-top:4px; color:#6f879b; font-size:12px; }

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
	flex-wrap: wrap;
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

.wallet-option { color: #526f8b; gap: 2vw; }
.wallet-option strong { flex: 1; font-size: 3.4vw; font-weight: 600; }
.payment-demo-badge { margin-left: auto; padding: 0.5vw 1.2vw; border-radius: 1vw; background: #edf6fc; color: #4b8bb6; font-size: 2.6vw; line-height: 1.4; }
.payment-demo-note { margin: 2vw 0 0; color: #8a9eac; font-size: 2.8vw; line-height: 1.5; }
.wallet-icon { width: 8vw; height: 8vw; display: grid; place-items: center; border-radius: 50%; background: #e8f5ff; color: #168bd1; font-size: 5vw; font-weight: 700; }
.asset-pay-hint { margin-top: 3vw; padding: 3vw; border: 1px solid #dcebf7; border-radius: 2vw; background: #f7fbff; color: #607b92; font-size: 3.2vw; line-height: 1.8; }
.asset-pay-hint label { display: flex; align-items: center; gap: 2vw; margin-top: 1vw; color: #315a79; }
.asset-pay-hint input { width: 28vw; border: 1px solid #c9deed; border-radius: 1.2vw; padding: 1.5vw 2vw; font-size: 3.2vw; color: #315a79; }
.asset-pay-hint small { display: block; color: #8aa1b4; }
.coupon-panel { margin-top: 3vw; padding-top: 3vw; border-top: 0.2vw solid #f5f7fa; }
.coupon-panel-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 2vw; }
.coupon-panel-header h3 { margin: 0; color: #333; font-size: 4.2vw; font-weight: 500; }
.coupon-panel-header span { color: #e76c48; font-size: 3.2vw; }
.coupon-option { width: 100%; display: flex; align-items: center; justify-content: space-between; gap: 3vw; margin-top: 2vw; padding: 3vw; border: 1px solid #e1edf5; border-radius: 1.8vw; background: #fbfdff; color: #31556d; text-align: left; cursor: pointer; }
.coupon-option.active { border-color: #78bde8; background: #f0f9ff; }
.coupon-option span { min-width: 0; flex: 1; }
.coupon-option b, .coupon-option small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.coupon-option b { color: #d96d49; font-size: 3.5vw; font-weight: 600; }
.coupon-option small { margin-top: 1vw; color: #8aa1b4; font-size: 2.9vw; }
.coupon-option i { flex: 0 0 auto; color: #168bd1; font-size: 4.5vw; }
.coupon-empty { padding: 3vw 0 1vw; color: #98aab7; font-size: 3.2vw; }
.coupon-discount { color: #df7049; }

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
	background: #0097ff;
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
/* 桌面窗口下仍保持移动端容器宽度，避免 vw 按整块屏幕放大文字。 */
.wrapper { max-width: 600px; margin: 0 auto; }
.wrapper header { width: min(100%, 600px); height: 64px; left: 50%; transform: translateX(-50%); font-size: 20px; }
.content { padding-top: 80px; padding-bottom: 104px; }
.section { border-radius: 12px; margin: 16px; padding: 20px; box-shadow: 0 2px 10px rgba(0, 0, 0, .05); }
.section-header { margin-bottom: 18px; }
.section-header h3 { font-size: 20px; }
.total-amount { font-size: 28px; }
.delivery-info { margin-bottom: 18px; padding-bottom: 14px; }
.info-item { margin-bottom: 10px; font-size: 15px; }
.info-item i { margin-right: 10px; width: 20px; }
.merchant-info { padding: 14px 0; }
.merchant-logo { width: 64px; height: 64px; border-radius: 8px; margin-right: 14px; }
.merchant-name { font-size: 17px; gap: 8px; }
.order-details { margin-top: 14px; padding-top: 14px; border-top-width: 1px; }
.detail-item { padding: 9px 0; font-size: 15px; }
.delivery-fee { border-top-width: 1px; margin-top: 9px; padding-top: 9px; }
.payment-options { gap: 12px; margin-top: 18px; }
.payment-option { padding: 16px; border-width: 1px; border-radius: 10px; }
.payment-option img { height: 32px; }
.payment-option .fa-check-circle { font-size: 20px; }
.wallet-option { gap: 10px; }
.wallet-option strong { font-size: 14px; }
.wallet-icon { width: 32px; height: 32px; font-size: 20px; }
.asset-pay-hint { margin-top: 14px; padding: 14px; border-radius: 10px; font-size: 13px; }
.asset-pay-hint label { gap: 8px; margin-top: 5px; }
.asset-pay-hint input { width: 120px; border-radius: 6px; padding: 7px 9px; font-size: 13px; }
.coupon-panel { margin-top: 14px; padding-top: 14px; border-top-width: 1px; }
.coupon-panel-header { margin-bottom: 10px; }
.coupon-panel-header h3 { font-size: 18px; }
.coupon-panel-header span { font-size: 13px; }
.coupon-option { gap: 12px; margin-top: 8px; padding: 12px; border-radius: 9px; }
.coupon-option b { font-size: 14px; }
.coupon-option small { margin-top: 4px; font-size: 12px; }
.coupon-option i { font-size: 18px; }
.coupon-empty { padding: 12px 0 4px; font-size: 13px; }
.payment-action { left: 50%; right: auto; width: min(100%, 600px); transform: translateX(-50%); padding: 16px; box-shadow: 0 -2px 10px rgba(0, 0, 0, .05); }
.pay-button { height: 48px; border-radius: 24px; font-size: 18px; }
.loading { font-size: 16px; }
</style>
