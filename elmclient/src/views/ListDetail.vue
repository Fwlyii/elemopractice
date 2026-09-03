<template>
	<div class="wrapper">
		<!-- 固定顶部栏 -->
		<div class="fixed-top">
			<div class="header">
				<p>订单详情</p>
			</div>
		</div>
		
		<!-- 内容区域 -->
		<div class="content-area">
			<!-- 加载提示 -->
			<div v-if="loading" class="loading">
				<p>加载中...</p>
			</div>
			
			<!-- 错误提示 -->
			<div v-else-if="error" class="error">
				<p>{{ error || ''}}</p>
				<button @click="retry">重新加载</button>
			</div>
			
			<!-- 订单详情内容 -->
			<div v-else class="content">
				<!-- 订单状态和基本信息 -->
				<div class="order-status">
					<div class="status-icon" :class="getStatusClass(orderDetail.orderState)">
						<i class="fa" :class="getStatusIcon(orderDetail.orderState)"></i>
					</div>
					<div class="status-info">
						<h3>{{ getStatusText(orderDetail.orderState) }}</h3>
						<p>订单号: {{ orderDetail.id || '-' }}</p>
						<p>下单时间: {{ formatTime(orderDetail.orderDate) || ''}}</p>
					</div>
				</div>

				<!-- 收货人信息 -->
				<div class="info-section">
					<h3 class="section-title">收货信息</h3>
					<div v-if="orderDetail.serviceMode !== 'PICKUP'" class="info-content">
						<p><span>收货人:</span> {{ orderDetail.contactName || '-' }} {{ getGenderText(orderDetail.contactSex) }}</p>
						<p><span>联系电话:</span> {{ orderDetail.contactTel || '-' }}</p>
						<p><span>配送地址:</span> {{ orderDetail.address || '-' }}</p>
					</div>
					<div v-else class="pickup-detail"><i class="fa fa-shopping-bag"></i><span>到店自取 · {{ orderDetail.businessAddress || orderDetail.businessName || '请到商家门店取餐' }}</span></div>
				</div>

				<div v-if="orderDetail.orderState === 7" class="info-section review-section">
					<h3 class="section-title">订单评价</h3>
					<div v-if="review" class="review-exists">
						<div class="stars">{{ '★'.repeat(review.rating) }}<span>{{ '★'.repeat(5-review.rating) }}</span></div>
						<p>{{ review.content || '用户未填写文字评价' }}</p>
						<p v-if="review.merchantReply" class="merchant-reply">商家回复：{{ review.merchantReply }}</p>
					</div>
					<div v-else class="review-form">
						<div class="star-picker"><button v-for="n in 5" :key="n" :class="{active:n<=reviewRating}" @click="reviewRating=n">★</button></div>
						<textarea v-model="reviewContent" maxlength="500" placeholder="说说这次用餐体验（选填，最多500字）"></textarea>
						<button class="review-submit" @click="submitReview" :disabled="reviewSubmitting">{{ reviewSubmitting ? '提交中...' : '提交评价' }}</button>
					</div>
				</div>

				<!-- 商家信息 -->
				<div class="info-section">
					<h3 class="section-title">商家信息</h3>
					<div class="info-content">
						<p><span>商家名称:</span> {{ orderDetail.businessName || '-' }}</p>
					</div>
				</div>

				<!-- 商品明细 -->
				<div class="info-section">
					<h3 class="section-title">商品明细</h3>
					<div class="items-list">
						<div v-for="(item, index) in orderDetail.foodList" :key="index" class="item-row">
							<div class="item-info">
								<span class="item-name">{{ item.foodName || '未知商品' }} &#165;{{ item.foodPrice }} &nbsp; × {{ item.quantity || 0 }}</span>
							</div>
							<div class="item-price">¥ {{ (Number(item.foodPrice || 0) * Number(item.quantity || 0)).toFixed(2) }}</div>
						</div>
					</div>
				</div>

				<!-- 费用汇总 -->
				<div class="info-section">
					<h3 class="section-title">费用明细</h3>
					<div class="price-details">
						<div class="price-row">
							<span>商品金额</span>
							<span>¥ {{ itemsTotal.toFixed(2) || '0'}}</span>
						</div>
						<div v-if="orderDetail.serviceMode !== 'PICKUP'" class="price-row">
							<span>配送费</span>
							<span>&#165;{{ Number(orderDetail.deliveryPrice || 0).toFixed(2) }}</span>
						</div>
						<div class="price-row total">
							<span>实付款</span>
							<span>¥ {{ Number(orderDetail.orderTotal || 0).toFixed(2) }}</span>
						</div>
					</div>
				</div>

				<!-- 操作按钮
				<div v-if="orderDetail.orderState === 0" class="action-buttons">
					<button class="btn cancel-btn" @click="cancelOrder">取消订单</button>
					<button class="btn pay-btn" @click="payOrder">立即支付</button>
				</div> -->
			</div>
		</div>
	</div>
</template>

<script>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '../utils/request';

export default {
	name: 'ListDetail',
	setup() {
		const route = useRoute();
		const router = useRouter();
		const orderId = ref(null);
		const orderDetail = ref({});
		const loading = ref(true);
		const error = ref('');
		const review = ref(null);
		const reviewRating = ref(5);
		const reviewContent = ref('');
		const reviewSubmitting = ref(false);
		
		// 配送费（这里假设固定值，实际应该从API获取）
		const deliveryPrice = ref(5);

		// 获取订单详情
		const fetchOrderDetail = async () => {
			loading.value = true;
			error.value = '';
			
			try {
				const response = await request.get("/api/orders/detail", {
					params: { orderId: orderId.value }
				});
				
				if (response.success) {
					orderDetail.value = response.data || {};
					if (orderDetail.value.orderState === 7) fetchReview();
					console.log("订单详情:", orderDetail.value);
				} else {
					error.value = '获取订单详情失败: ' + response.message;
				}
			} catch (err) {
				console.error('获取订单详情失败:', err);
				error.value = '网络错误，请稍后重试';
			} finally {
				loading.value = false;
			}
		};
		const fetchReview = async () => {
			try { const res = await request.get(`/api/v1/reviews/order/${orderId.value}`); if (res.success) review.value = res.data || null; } catch (e) { console.warn('评价查询失败', e); }
		};
		const submitReview = async () => {
			reviewSubmitting.value = true;
			try { const res = await request.post('/api/v1/reviews', { orderId: orderId.value, rating: reviewRating.value, content: reviewContent.value }); if (res.success) { review.value=res.data; alert('评价提交成功'); } else alert(res.message || '评价提交失败'); } catch (e) { alert(e?.message || '评价提交失败'); } finally { reviewSubmitting.value=false; }
		};

		// 重新加载
		const retry = () => {
			fetchOrderDetail();
		};

		// 获取状态文本
		const getStatusText = (state) => {
			const statusMap = {
				0: "待支付", 1: "待商家接单", 2: "制作中", 3: "待骑手接单",
				4: orderDetail.value.serviceMode === 'PICKUP' ? "待到店自取" : "待骑手取餐",
				5: "配送中", 6: "已送达·待确认", 7: "已完成", 8: "已取消", 9: "配送异常"
			};
			return statusMap[state] || "未知状态";
		};

		// 获取状态样式类
		const getStatusClass = (state) => {
			const classMap = { 0: "status-unpaid", 1: "status-pending", 2: "status-accepted", 3: "status-accepted", 4: "status-accepted", 5: "status-accepted", 6: "status-accepted", 7: "status-done", 8: "status-canceled", 9: "status-unpaid" };
			return classMap[state] || "status-unknown";
		};

		// 获取状态图标
		const getStatusIcon = (state) => {
			const iconMap = {
				0: "fa-clock-o",
				1: "fa-hourglass-half",
				2: "fa-check-circle",
				3: "fa-check-circle",
				4: "fa-shopping-bag",
				5: "fa-motorcycle",
				6: "fa-check-circle",
				7: "fa-check-circle",
				8: "fa-times-circle",
				9: "fa-exclamation-triangle"
			};
			return iconMap[state] || "fa-question-circle";
		};

		// 获取性别文本
		const getGenderText = (gender) => {
			return gender === 1 ? '先生' : gender === 2 ? '女士' : '';
		};

		// 格式化时间
		const formatTime = (timeString) => {
			if (!timeString) return "-";
			try {
				const date = new Date(timeString);
				return date.toLocaleString('zh-CN');
			} catch (e) {
				return timeString;
			}
		};

		// 计算商品总金额
		const itemsTotal = computed(() => {
			if (!orderDetail.value.foodList || !Array.isArray(orderDetail.value.foodList)) {
				return 0;
			}
			return orderDetail.value.foodList.reduce((total, item) => {
				return total + Number(item.foodPrice || 0) * Number(item.quantity || 0);
			}, 0);
		});

		// 取消订单
		const cancelOrder = async () => {
			if (!confirm("确定要取消此订单吗？")) return;
			
			try {
				const response = await request.post("/api/orders/cancel", { 
					orderId: orderId.value 
				});
				
				if (response.data.success) {
					alert("订单取消成功");
					// 重新加载订单详情
					fetchOrderDetail();
				} else {
					alert("取消失败: " + response.data.message);
				}
			} catch (err) {
				console.error("取消订单失败:", err);
				alert("取消订单失败，请稍后重试");
			}
		};

		// 支付订单
		const payOrder = () => {
			router.push({ 
				path: "/payment", 
				query: { orderId: orderId.value } 
			});
		};

		onMounted(() => {
			orderId.value = route.query.orderId;
			if (!orderId.value) {
				error.value = "订单ID不能为空";
				loading.value = false;
				return;
			}
			
			fetchOrderDetail();
		});

		return {
			orderId,
			orderDetail,
			loading,
			error,
			deliveryPrice,
			itemsTotal,
			fetchOrderDetail,
			retry,
			getStatusText,
			getStatusClass,
			getStatusIcon,
			getGenderText,
			formatTime,
			cancelOrder,
			payOrder
			,review,reviewRating,reviewContent,reviewSubmitting,submitReview
		};
	}
};
</script>

<style scoped>
.wrapper {
	width: 100%;
	min-height: 100vh;
	background: #f5f7fa;
}

/****************** 固定顶部栏 ******************/
.fixed-top {
	position: fixed;
	top: 0;
	left: 0;
	width: 100%;
	z-index: 1000;
	background: white;
}

.header {
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
  justify-content: center;
  align-items: center;
}

.title {
	margin: 0;
	font-size: 24px;
	font-weight: 600;
	color: white;
}

/****************** 内容区域 ******************/
.content-area {
	margin-top: 50px; /* 固定顶部栏的高度 */
	padding-bottom: 20vw;
}

.loading, .error {
	display: flex;
	flex-direction: column;
	justify-content: center;
	align-items: center;
	padding: 20vw 4vw;
	font-size: 4vw;
	color: #666;
}

.error button {
	margin-top: 4vw;
	padding: 2vw 6vw;
	background: #409eff;
	color: white;
	border: none;
	border-radius: 1vw;
	cursor: pointer;
}

/* 订单状态区域 */
.order-status {
	display: flex;
	align-items: center;
	padding: 6vw 4vw;
	background: white;
	margin-bottom: 3vw;
}

.status-icon {
	width: 16vw;
	height: 16vw;
	border-radius: 50%;
	display: flex;
	justify-content: center;
	align-items: center;
	margin-right: 4vw;
	font-size: 8vw;
}

.status-unpaid {
	background: #fff0f0;
	color: #ff4d4f;
}

.status-pending {
	background: #e6f7ff;
	color: #1890ff;
}

.status-accepted {
	background: #f6ffed;
	color: #52c41a;
}

.status-done {
	background: #f9f9f9;
	color: #999;
}

.status-canceled {
	background: #f9f9f9;
	color: #999;
}

.status-unknown {
	background: #f9f9f9;
	color: #666;
}

.status-info h3 {
	font-size: 4.5vw;
	color: #333;
	margin-bottom: 1vw;
	font-weight: bold;
}

.status-info p {
	font-size: 3.6vw;
	color: #666;
	margin: 0.5vw 0;
}

/* 信息区块 */
.info-section {
	background: white;
	margin-bottom: 3vw;
	padding: 4vw;
}

.section-title {
	font-size: 4.2vw;
	color: #333;
	margin-bottom: 3vw;
	font-weight: bold;
	border-bottom: 1px solid #f0f0f0;
	padding-bottom: 2vw;
}

.info-content p {
	font-size: 3.8vw;
	color: #333;
	margin: 2vw 0;
	display: flex;
}
.pickup-detail { display:flex; align-items:center; gap:10px; padding:12px 14px; color:#168bd1; background:#f5fbff; border:1px solid #d9ecf8; border-radius:10px; }
.pickup-detail i { font-size:20px; }

.info-content span {
	color: #666;
	margin-right: 2vw;
	min-width: 20vw;
}

/* 商品列表 */
.items-list {
	border-top: 1px solid #f0f0f0;
}

.item-row {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 3vw 0;
	border-bottom: 1px solid #f0f0f0;
}

.item-info {
	flex: 1;
}

.item-name {
	font-size: 3.8vw;
	color: #333;
}

.item-quantity {
	font-size: 3.4vw;
	color: #666;
	margin-left: 2vw;
}

.item-price {
	font-size: 3.8vw;
	color: #333;
	font-weight: 500;
}

/* 价格明细 */
.price-details {
	border-top: 1px solid #f0f0f0;
	padding-top: 3vw;
}

.price-row {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 2vw 0;
	font-size: 3.8vw;
	color: #333;
}

.price-row.total {
	border-top: 1px solid #f0f0f0;
	margin-top: 2vw;
	padding-top: 3vw;
	font-weight: bold;
	font-size: 4.2vw;
	color: #ff6b00;
}

/* 操作按钮 */
.action-buttons {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	background: white;
	padding: 3vw 4vw;
	display: flex;
	justify-content: flex-end;
	gap: 3vw;
	border-top: 1px solid #f0f0f0;
}

.btn {
	padding: 3vw 6vw;
	border-radius: 1.6vw;
	font-size: 3.8vw;
	cursor: pointer;
	border: none;
}

.cancel-btn {
	background: #fff;
	color: #666;
	border: 1px solid #ddd;
}

.pay-btn {
	background: #409eff;
	color: #fff;
}
</style>

<style scoped>
.review-form textarea{width:100%;min-height:76px;border:1px solid #dbe7f0;border-radius:8px;padding:10px;box-sizing:border-box;resize:vertical;font:inherit}.star-picker{display:flex;gap:6px;margin:4px 0 10px}.star-picker button{border:0;background:none;color:#c4d0da;font-size:28px;padding:0;cursor:pointer}.star-picker button.active{color:#f4b63e}.review-submit{margin-top:10px;border:0;border-radius:7px;background:#168bd1;color:#fff;padding:9px 18px}.review-submit:disabled{opacity:.6}.stars{color:#f4b63e;letter-spacing:2px}.stars span{color:#ccd8e2;letter-spacing:2px}.review-exists p{color:#52697c;line-height:1.6;margin:8px 0}.merchant-reply{padding:9px 12px;background:#f5f9fc;border-left:3px solid #168bd1;font-size:13px}
</style>
