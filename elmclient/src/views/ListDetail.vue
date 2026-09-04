<template>
	<div class="wrapper">
		<!-- 固定顶部栏 -->
		<div class="fixed-top">
			<div class="header">
				<button type="button" class="header-back" aria-label="返回" @click="router.back()">‹</button>
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

				<div v-if="orderDetail.orderState === 7" ref="reviewSection" class="info-section review-section">
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
import { ref, onMounted, computed, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '../utils/request';
import { toast } from '../utils/toast';

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
		const reviewSection = ref(null);
		
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
					if (orderDetail.value.orderState === 7) {
						await fetchReview();
					}
					console.log("订单详情:", orderDetail.value);
				} else {
					error.value = '获取订单详情失败: ' + response.message;
				}
			} catch (err) {
				console.error('获取订单详情失败:', err);
				error.value = '网络错误，请稍后重试';
			} finally {
				loading.value = false;
				if (route.query.focus === 'review' && orderDetail.value.orderState === 7) {
					await nextTick();
					reviewSection.value?.scrollIntoView({ behavior: 'auto', block: 'center' });
				}
			}
		};
		const fetchReview = async () => {
			try { const res = await request.get(`/api/v1/reviews/order/${orderId.value}`); if (res.success) review.value = res.data || null; } catch (e) { console.warn('评价查询失败', e); }
		};
		const submitReview = async () => {
			reviewSubmitting.value = true;
			try { const res = await request.post('/api/v1/reviews', { orderId: orderId.value, rating: reviewRating.value, content: reviewContent.value }); if (res.success) { review.value=res.data; toast.success('评价提交成功'); } else toast.error(res.message || '评价提交失败'); } catch (e) { toast.error(e?.response?.data?.message || e?.message || '评价提交失败'); } finally { reviewSubmitting.value=false; }
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
			payOrder,
			router,
			review,reviewRating,reviewContent,reviewSubmitting,reviewSection,submitReview
		};
	}
};
</script>

<style scoped>
.wrapper{width:100%;max-width:720px;margin:0 auto;min-height:100vh;background:#f5f7fa;color:#2d4354}
.fixed-top{position:fixed;top:0;left:50%;transform:translateX(-50%);width:min(100%,720px);height:56px;z-index:1000;background:#0097ff}
.header{width:100%;height:56px;background:#0097ff;color:#fff;font-size:18px;font-weight:600;position:relative;display:flex;justify-content:center;align-items:center}
.header-back{position:absolute;left:12px;top:50%;transform:translateY(-50%);border:0;background:transparent;color:#fff;font-size:32px;line-height:1;padding:4px 8px;cursor:pointer}
.content-area{padding:70px 0 28px}.loading,.error{display:flex;flex-direction:column;justify-content:center;align-items:center;padding:80px 20px;font-size:15px;color:#667b8b}.error button{margin-top:16px;padding:9px 22px;background:#409eff;color:#fff;border:0;border-radius:6px;cursor:pointer}
.order-status{display:flex;align-items:center;padding:22px 18px;background:#fff;margin:0 16px 10px;border:1px solid #e5edf3;border-radius:10px}.status-icon{width:62px;height:62px;border-radius:50%;display:flex;justify-content:center;align-items:center;margin-right:16px;font-size:30px;flex:0 0 62px}.status-unpaid{background:#fff0f0;color:#ff4d4f}.status-pending{background:#e6f7ff;color:#1890ff}.status-accepted{background:#f6ffed;color:#52c41a}.status-done,.status-canceled{background:#f9f9f9;color:#999}.status-unknown{background:#f9f9f9;color:#666}.status-info h3{font-size:19px;color:#333;margin-bottom:5px;font-weight:700}.status-info p{font-size:13px;color:#667b8b;margin:3px 0}
.info-section{background:#fff;margin:0 16px 10px;padding:16px 18px;border:1px solid #e5edf3;border-radius:10px}.section-title{font-size:17px;color:#333;margin-bottom:12px;font-weight:700;border-bottom:1px solid #edf1f4;padding-bottom:9px}.info-content p{font-size:14px;color:#333;margin:9px 0;display:flex;line-height:1.55}.info-content span{color:#6c7d8a;margin-right:10px;min-width:88px}.pickup-detail{display:flex;align-items:center;gap:10px;padding:12px 14px;color:#168bd1;background:#f5fbff;border:1px solid #d9ecf8;border-radius:10px}.pickup-detail i{font-size:20px}
.items-list,.price-details{border-top:1px solid #edf1f4}.item-row{display:flex;justify-content:space-between;align-items:center;gap:14px;padding:12px 0;border-bottom:1px solid #edf1f4}.item-info{flex:1;min-width:0}.item-name,.item-price{font-size:14px;color:#333}.item-price{font-weight:500;white-space:nowrap}.item-quantity{font-size:13px;color:#666;margin-left:8px}.price-details{padding-top:12px}.price-row{display:flex;justify-content:space-between;align-items:center;padding:8px 0;font-size:14px;color:#333}.price-row.total{border-top:1px solid #edf1f4;margin-top:8px;padding-top:12px;font-weight:700;font-size:17px;color:#ff6b00}
.action-buttons{position:fixed;bottom:0;left:50%;transform:translateX(-50%);width:min(100%,720px);box-sizing:border-box;background:#fff;padding:12px 16px;display:flex;justify-content:flex-end;gap:12px;border-top:1px solid #edf1f4}.btn{padding:9px 22px;border-radius:6px;font-size:14px;cursor:pointer;border:0}.cancel-btn{background:#fff;color:#666;border:1px solid #ddd}.pay-btn{background:#409eff;color:#fff}
@media(max-width:480px){.order-status{padding:17px 14px}.status-icon{width:52px;height:52px;flex-basis:52px;font-size:25px}.info-section{padding:14px}.info-content p{font-size:13px}.info-content span{min-width:76px}.item-row{align-items:flex-start}.item-name,.item-price{font-size:13px}}
</style>

<style scoped>
.review-form textarea{width:100%;min-height:76px;border:1px solid #dbe7f0;border-radius:8px;padding:10px;box-sizing:border-box;resize:vertical;font:inherit}.star-picker{display:flex;gap:6px;margin:4px 0 10px}.star-picker button{border:0;background:none;color:#c4d0da;font-size:28px;padding:0;cursor:pointer}.star-picker button.active{color:#f4b63e}.review-submit{margin-top:10px;border:0;border-radius:7px;background:#168bd1;color:#fff;padding:9px 18px}.review-submit:disabled{opacity:.6}.stars{color:#f4b63e;letter-spacing:2px}.stars span{color:#ccd8e2;letter-spacing:2px}.review-exists p{color:#52697c;line-height:1.6;margin:8px 0}.merchant-reply{padding:9px 12px;background:#f5f9fc;border-left:3px solid #168bd1;font-size:13px}
</style>
