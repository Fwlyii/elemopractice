<template>
    <div class="wrapper">
        <header class="store-header">
            <button class="back-button" type="button" aria-label="返回" @click="goBack">‹</button>
            <div class="service-switch" role="tablist" aria-label="配送方式">
                <button type="button" :class="{ active: deliveryMode === 'delivery' }" @click="setDeliveryMode('delivery')">外送</button>
                <button type="button" :class="{ active: deliveryMode === 'pickup' }" :disabled="business.dineInAvailable === false" @click="setDeliveryMode('pickup')">自取</button>
            </div>
            <div class="header-actions">
                <button type="button" title="搜索商品" @click="focusMenu"><i class="fa fa-search"></i></button>
                <button type="button" title="收藏商家" :class="{ active: isFavorited }" @click.stop="toggleFavorite"><i class="fa fa-star"></i></button>
            </div>
        </header>

        <section class="store-hero">
            <img class="business-logo" :src="business.businessImg || require('@/assets/business-default.png')" :alt="business.businessName || '商家图片'" @error="handleImageError" />
            <div class="business-info">
                <div class="business-title-row">
                    <h1>{{ business.businessName || '商家' }}</h1>
                    <span class="open-badge" :class="{ closed: !isBusinessOpen }">{{ business.status !== undefined && business.status !== 1 ? '暂未上线' : business.operatingStatus === false ? '休息中' : '营业中' }}</span>
                </div>
                <p class="business-meta">{{ deliveryMode === 'pickup' ? '到店自取 · 无起送门槛' : `起送 ¥${formatMoney(business.startPrice)} · 配送 ¥${formatMoney(business.deliveryPrice)}` }}</p>
                <p class="business-address"><i class="fa fa-map-marker"></i>{{ business.businessAddress || '校园周边配送' }}</p>
            </div>
            <div class="hero-reactions">
                <button type="button" :class="{ active: isLiked }" @click.stop="toggleLike"><i class="fa fa-thumbs-up"></i><span>{{ isLiked ? '已赞' : '点赞' }}</span></button>
                <button type="button" :class="{ active: isFavorited }" @click.stop="toggleFavorite"><i class="fa fa-star"></i><span>{{ isFavorited ? '已收藏' : '收藏' }}</span></button>
            </div>
        </section>

        <div class="offer-strip" aria-label="商家优惠">
            <span v-if="deliveryMode === 'pickup'">自取免配送费</span><template v-else><span v-if="Number(business.deliveryPrice || 0) === 0">免配送费</span><span v-else>配送 ¥{{ formatMoney(business.deliveryPrice) }}</span></template><span v-if="business.promotionThreshold && business.promotionDiscount">满{{ formatMoney(business.promotionThreshold) }}减{{ formatMoney(business.promotionDiscount) }}</span><span>品质保障</span><span v-if="business.dineInAvailable">支持自取</span>
        </div>

        <nav class="page-tabs" role="tablist" aria-label="商家内容">
            <button type="button" role="tab" :aria-selected="activeTab === 'order'" :class="{ active: activeTab === 'order' }" @click="selectTab('order')">点餐</button>
            <button type="button" role="tab" :aria-selected="activeTab === 'reviews'" :class="{ active: activeTab === 'reviews' }" @click="selectTab('reviews')">评价 <small v-if="reviews.length">{{ reviews.length }}</small></button>
            <button type="button" role="tab" :aria-selected="activeTab === 'story'" :class="{ active: activeTab === 'story' }" @click="selectTab('story')">商家故事</button>
        </nav>

        <main class="page-content">
            <section v-if="activeTab === 'order'" class="order-panel" aria-label="点餐">
                <div class="mode-hint">
                    <i class="fa" :class="deliveryMode === 'pickup' ? 'fa-shopping-bag' : 'fa-motorcycle'"></i>
                    <div><strong>{{ deliveryMode === 'pickup' ? '到店自取' : '外送到家' }}</strong><span>{{ deliveryMode === 'pickup' ? '下单后到门店取餐，预计 15 分钟' : '专人配送，预计 30 分钟送达' }}</span></div>
                    <button type="button" @click="setDeliveryMode(deliveryMode === 'pickup' ? 'delivery' : 'pickup')">切换</button>
                </div>
                <div class="section-heading"><h2>菜单</h2><span>{{ foodArr.length }} 件商品</span></div>
                <div v-if="loadingFoods" class="state-card">正在加载菜单…</div>
                <div v-else-if="!foodArr.length" class="state-card">暂时没有可售商品</div>
                <div v-else class="menu-layout">
                    <nav class="category-sidebar" aria-label="商品分类">
                        <button v-for="category in menuCategories" :key="category" type="button"
                            :class="{ active: activeCategory === category }" @click="activeCategory = category">
                            {{ category }}
                        </button>
                    </nav>
                    <div class="category-content">
                        <h3 class="category-title">{{ activeCategory }}</h3>
                        <ul class="food">
                            <li v-for="item in visibleFoods" :key="item.foodId">
                                <div class="food-left">
                                    <img :src="item.foodImg || require('@/assets/food-default.png')" :alt="item.foodName" @error="handleImageError" />
                                    <div class="food-left-info">
                                        <h3>{{ item.foodName || '' }}</h3>
                                        <p class="food-explain">{{ item.foodExplain || '商家精选，现点现做' }}</p>
                                        <p class="food-price"><span>¥{{ formatMoney(item.foodPrice) }}</span><small v-if="item.purchaseLimit" class="food-limit">限购 {{ item.purchaseLimit }} 份</small><small v-if="Number(item.stock || 0) <= 0" class="sold-out-label">已售罄</small></p>
                                    </div>
                                </div>
                                <div class="food-right">
                                    <button type="button" class="quantity-btn minus-btn" v-show="getCartQuantity(item.id) > 0" @click="minus(item)" aria-label="减少数量">−</button>
                                    <span v-show="getCartQuantity(item.id) > 0" class="quantity">{{ getCartQuantity(item.id) }}</span>
                                    <button type="button" class="quantity-btn plus-btn"
                                        :disabled="isSoldOut(item) || isCartQuantityAtLimit(item)"
                                        :class="{ disabled: isCartQuantityAtLimit(item) }"
                                        :title="isCartQuantityAtLimit(item) ? cartQuantityLimitMessage(item) : ''"
                                        @click="add(item)"
                                        :aria-label="isCartQuantityAtLimit(item) ? `${item.foodName}已达限购数量` : `增加${item.foodName}数量`">＋</button>
                                </div>
                            </li>
                        </ul>
                    </div>
                </div>
            </section>

            <section v-else-if="activeTab === 'reviews'" class="reviews-panel" aria-label="评价">
                <div class="rating-summary">
                    <div class="rating-score">{{ reviewAverage }}</div>
                    <div><div class="rating-stars">{{ reviews.length ? reviewStars(Math.round(Number(reviewAverage))) : '暂无评分' }}</div><span>综合评分</span></div>
                    <div class="rating-count">{{ reviews.length }} 条评价</div>
                </div>
                <div v-if="loadingReviews" class="state-card">正在加载评价…</div>
                <div v-else-if="!reviews.length" class="state-card">还没有评价，欢迎成为第一位评价的顾客</div>
                <article v-for="review in reviews" v-else :key="review.id" class="review-card">
                    <div class="review-head"><strong>{{ review.customerName || '匿名用户' }}</strong><span class="review-stars">{{ reviewStars(review.rating) }}</span><time>{{ formatDate(review.createTime) }}</time></div>
                    <p>{{ review.content || '用户未填写文字评价' }}</p>
                    <div v-if="review.merchantReply" class="merchant-reply">商家回复：{{ review.merchantReply }}</div>
                </article>
            </section>

            <section v-else class="story-panel" aria-label="商家故事">
                <div class="story-card"><span class="story-label">BRAND STORY</span><h2>{{ business.businessName || '这家店' }}</h2><p>{{ business.businessExplain || '认真做好每一份餐点，把新鲜和热乎送到你手上。' }}</p></div>
                <div class="store-details"><h3>门店信息</h3><div><i class="fa fa-clock-o"></i><span><b>营业时间</b><em>每天 09:00 - 21:30</em></span></div><div><i class="fa fa-map-marker"></i><span><b>门店地址</b><em>{{ business.businessAddress || '校园周边' }}</em></span></div><div><i class="fa fa-shield"></i><span><b>服务承诺</b><em>食安保障 · 售后无忧</em></span></div></div>
            </section>
        </main>

        <div v-if="activeTab === 'order'" class="cart">
            <div class="cart-left" @click="goToCart">
                <div class="cart-left-icon" :class="{ filled: totalQuantity > 0 }"><i class="fa fa-shopping-cart"></i><div v-if="totalQuantity" class="cart-left-icon-quantity">{{ totalQuantity }}</div></div>
                <div class="cart-left-info"><p>¥{{ formatMoney(totalPrice) }}</p><span>{{ deliveryMode === 'pickup' ? '到店自取 · 免配送费' : `另需配送费 ¥${formatMoney(business.deliveryPrice)}` }}</span></div>
            </div>
            <div class="cart-right"><button type="button" class="cart-right-item" :class="{ ready: canOrder }" :disabled="!canOrder" @click="toOrder">{{ orderButtonText }}</button></div>
        </div>
    </div>
</template>

<script>
import { ref, onMounted, computed, watch, onErrorCaptured } from "vue";
import { useRoute, useRouter } from "vue-router";
import request from "@/utils/request";
import { toast } from '@/utils/toast';
import { formatDate, formatMoney } from '@/utils/formatters';
import { getToken, updateStoredUser } from '@/utils/auth';
import { cartQuantityLimitMessage, isSoldOut, maxCartQuantity } from '@/utils/cartQuantityRules';
import { addCartItem, listCartItems, removeCartItem, setCartItemQuantity } from '@/services/cartService';
import { getMyInteraction, updateMyInteraction } from '@/services/merchantInteractionService';
export default {
    name: "BusinessInfo",
    setup() {
        const route = useRoute();
        const router = useRouter();
        const userInfo = ref(null);

        // 基础数据
        const businessId = ref(null);
        const business = ref({
            id: 0,
            businessName: "",
            businessImg: "",
            startPrice: 0,
            deliveryPrice: 0,
            businessExplain: "",
            businessAddress: "",
            orderTypeId: 0,
            remarks: ""
        });
        const foodArr = ref([]);
        const activeCategory = ref('');
        const cartItems = ref([]); // 购物车商品列表
        const loadingBusiness = ref(false);
        const loadingFoods = ref(false);
        const loadingCart = ref(false);

        // 用户交互状态
        const isLiked = ref(false);
        const isFavorited = ref(false);
        const interactionLoading = ref(false);
        const activeTab = ref('order');
        const deliveryMode = ref('delivery');
        const reviews = ref([]);
        const loadingReviews = ref(false);
        const requireLogin = (message = '登录后即可继续') => {
            toast.info(message);
            router.push({ path: '/login', query: { role: 'user', redirect: route.fullPath } });
        };

        const reviewStars = (rating) => {
            const score = Math.max(0, Math.min(5, Number(rating || 0)));
            return '★'.repeat(score) + '☆'.repeat(5 - score);
        };
        const reviewAverage = computed(() => {
            if (!reviews.value.length) return '—';
            return (reviews.value.reduce((sum, item) => sum + Number(item.rating || 0), 0) / reviews.value.length).toFixed(1);
        });
        const menuCategories = computed(() => {
            const categories = [];
            foodArr.value.forEach(item => {
                const category = String(item.category || '其他').trim() || '其他';
                if (!categories.includes(category)) categories.push(category);
            });
            return categories;
        });
        const visibleFoods = computed(() => foodArr.value.filter(item =>
            (String(item.category || '其他').trim() || '其他') === activeCategory.value));
        watch(menuCategories, categories => {
            if (!categories.includes(activeCategory.value)) activeCategory.value = categories[0] || '';
        }, { immediate: true });
        const setDeliveryMode = (mode) => {
            if (mode === 'pickup' && business.value.dineInAvailable === false) {
                toast.warning('该商家暂不支持到店自取');
                return;
            }
            deliveryMode.value = mode;
            if (businessId.value) localStorage.setItem(`businessServiceMode:${businessId.value}`, mode);
        };
        const selectTab = (tab) => {
            activeTab.value = tab;
            if (tab === 'reviews' && !reviews.value.length) loadReviews();
        };
        const focusMenu = () => {
            activeTab.value = 'order';
            window.setTimeout(() => document.querySelector('.menu-layout')?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 0);
        };
        const goBack = () => router.back();
        const handleImageError = (event) => {
            // 远程图片失效时使用本地占位图，避免详情页出现破图图标和拥挤的替代文字。
            if (event.target.dataset.fallbackApplied) return;
            event.target.dataset.fallbackApplied = 'true';
            const isFoodImage = event.target.closest('.food');
            event.target.src = isFoodImage ? require('@/assets/food-default.png') : require('@/assets/business-default.png');
        };

        const loadReviews = async () => {
            if (!businessId.value || loadingReviews.value) return;
            loadingReviews.value = true;
            try {
                const response = await request.get(`/api/v1/reviews/business/${businessId.value}`);
                if (response?.success) reviews.value = response.data || [];
            } catch (error) {
                console.error('获取商家评价失败:', error);
                reviews.value = [];
            } finally {
                loadingReviews.value = false;
            }
        };

        const fetchUserInfo = async () => {
            if (!getToken()) return;

            try {
                const res = await request.get('/api/user');
                if (res) {
                    userInfo.value = res;
                    updateStoredUser(res);
                } else {
                    console.error('获取用户信息失败');
                    userInfo.value = null;
                }
            } catch (error) {
                console.error('获取用户信息异常:', error);
                userInfo.value = null;
            }
        };

        // 错误捕获
        onErrorCaptured((error) => {
            console.error('组件错误捕获:', error);
            return false;
        });

        // 获取购物车列表
        const fetchCartList = async () => {
            if (!userInfo.value?.id || !businessId.value) {
                cartItems.value = [];
                return;
            }

            loadingCart.value = true;
            try {
                cartItems.value = await listCartItems(businessId.value) || [];
            } catch (error) {
                console.error("获取购物车列表失败:", error);
                cartItems.value = [];
            } finally {
                loadingCart.value = false;
            }
        };

        // 获取指定食品在购物车中的数量
        const getCartQuantity = (foodId) => {
            const cartItem = cartItems.value.find(item => item.foodId === foodId);
            return cartItem ? cartItem.quantity : 0;
        };
        const isCartQuantityAtLimit = (food) => getCartQuantity(food.id) >= maxCartQuantity(food);

        // 添加商品到购物车
        const addToCart = async (food) => {
            // 检查用户是否登录
            if (!userInfo.value?.id) {
                requireLogin('登录后即可加入购物车');
                return;
            }
            if (getCartQuantity(food.id) >= maxCartQuantity(food)) {
                toast.warning(cartQuantityLimitMessage(food));
                return;
            }

            try {
                const currentQuantity = getCartQuantity(food.id);
                const newQuantity = currentQuantity + 1;

                if (currentQuantity > 0) {
                    // 如果商品已在购物车中，更新数量
                    await updateCartItem(food.id, newQuantity);
                } else {
                    // 如果商品不在购物车中，添加新商品
                    await addNewCartItem(food.id);
                }

                // 重新获取购物车列表以更新显示
                await fetchCartList();
            } catch (error) {
                console.error('添加商品到购物车失败:', error);
                toast.error(error?.response?.data?.message || error?.message || '添加商品失败，请重试');
            }
        };

        // 添加新商品到购物车
        const addNewCartItem = async (foodId) => {
            try {
                await addCartItem(foodId, 1);
                return true;
            } catch (error) {
                console.error('添加新商品到购物车失败:', error);
                throw error;
            }
        };

        // 更新购物车商品数量
        const updateCartItem = async (foodId, newQuantity) => {
            try {
                // 先找到对应的购物车项ID
                const cartItem = cartItems.value.find(item => item.foodId === foodId);
                if (!cartItem) {
                    throw new Error(`未找到foodId ${foodId}对应的购物车项`);
                }

                await setCartItemQuantity(cartItem.id, newQuantity);
                return true;
            } catch (error) {
                console.error('更新购物车商品数量失败:', error);
                throw error;
            }
        };

        // 从购物车移除商品（减少数量）
        const removeFromCart = async (food) => {
            // 检查用户是否登录
            if (!userInfo.value?.id) {
                requireLogin('登录后即可调整购物车');
                return;
            }

            try {
                const currentQuantity = getCartQuantity(food.id);
                if (currentQuantity <= 0) {
                    return;
                }

                const newQuantity = currentQuantity - 1;

                if (newQuantity <= 0) {
                    // 如果数量为0，从购物车中删除
                    await deleteCartItem(food.id);
                } else {
                    // 减少数量
                    await updateCartItem(food.id, newQuantity);
                }

                // 重新获取购物车列表以更新显示
                await fetchCartList();
            } catch (error) {
                console.error('从购物车移除商品失败:', error);
                toast.error('移除商品失败，请重试');
            }
        };

        // 从购物车删除商品
        const deleteCartItem = async (foodId) => {
            try {
                const cartItem = cartItems.value.find(item => item.foodId === foodId);
                if (!cartItem) {
                    return;
                }

                await removeCartItem(cartItem.id);
                return true;
            } catch (error) {
                console.error('从购物车删除商品失败:', error);
                throw error;
            }
        };

        // 加载用户互动状态
        const loadReactions = async () => {
            try {
                if (!businessId.value) {
                    console.error("缺少businessId");
                    return;
                }

                let retry = 0;
                while (!userInfo.value?.id && retry < 4) {
                    await new Promise(resolve => setTimeout(resolve, 500));
                    retry++;
                }

                const userId = userInfo.value?.id;
                if (!userId) {
                    isLiked.value = false;
                    isFavorited.value = false;
                    return;
                }

                const interaction = await getMyInteraction(businessId.value);
                isLiked.value = Boolean(interaction?.liked);
                isFavorited.value = Boolean(interaction?.collected);
            } catch (error) {
                console.error("加载互动状态异常:", error);
                isLiked.value = false;
                isFavorited.value = false;
            }
        };
        // 更新互动状态到后端
        const updateInteraction = async (type, newValue) => {
            try {
                // 如果已经是目标状态，则不再执行
                if ((type === 'like' && isLiked.value === newValue) ||
                    (type === 'favorite' && isFavorited.value === newValue)) {
                    return;
                }

                interactionLoading.value = true;
                const userId = userInfo.value?.id;
                if (!userId) {
                    toast.error('请先登录');
                    return;
                }

                const dto = {
                    merchantId: businessId.value,
                    liked: type === 'like' ? newValue : isLiked.value,
                    collected: type === 'favorite' ? newValue : isFavorited.value
                };

                await updateMyInteraction(dto);
                if (type === 'like') {
                    isLiked.value = newValue;
                } else {
                    isFavorited.value = newValue;
                }
            } catch (error) {
                console.error(`${type}状态更新异常:`, error);
                toast.error('操作异常，请检查网络');
            } finally {
                interactionLoading.value = false;
            }
        };


        // 修改切换函数，增加状态检查
        const toggleLike = async () => {
            if (interactionLoading.value) return;
            if (!userInfo.value?.id) {
                requireLogin('登录后即可点赞');
                return;
            }
            await updateInteraction('like', !isLiked.value);
        };

        const toggleFavorite = async () => {
            if (interactionLoading.value) return;
            if (!userInfo.value?.id) {
                requireLogin('登录后即可收藏');
                return;
            }
            await updateInteraction('favorite', !isFavorited.value);
        };



        // 获取商家信息
        const fetchBusinessInfo = async () => {
            loadingBusiness.value = true;
            try {
                const response = await request.get(`/api/businesses/${businessId.value}`);

                if (response.success === true) {
                    business.value = {
                        id: response.data.id,
                        businessName: response.data.businessName,
                        businessImg: response.data.businessImg,
                        startPrice: response.data.startPrice,
                        deliveryPrice: response.data.deliveryPrice,
                        dineInAvailable: response.data.dineInAvailable,
                        status: response.data.status,
                        operatingStatus: response.data.operatingStatus,
                        promotionThreshold: response.data.promotionThreshold,
                        promotionDiscount: response.data.promotionDiscount,
                        businessExplain: response.data.businessExplain,
                        businessAddress: response.data.businessAddress,
                        orderTypeId: response.data.orderTypeId,
                        remarks: response.data.remarks
                    };
                    const savedMode = localStorage.getItem(`businessServiceMode:${businessId.value}`) || 'delivery';
                    deliveryMode.value = savedMode === 'pickup' && response.data.dineInAvailable === false ? 'delivery' : savedMode;
                } else {
                    const errorMsg = response.message || "获取商家信息失败";
                    console.error("商家信息API返回失败:", errorMsg);
                    throw new Error(errorMsg);
                }
            } catch (error) {
                console.error("获取商家信息失败:", error);
            } finally {
                loadingBusiness.value = false;
            }
        };

        // 获取食品列表
        const fetchFoodList = async () => {
            loadingFoods.value = true;
            try {
                const response = await request.get("/api/foods/list", {
                    params: { businessId: businessId.value }
                });
                if (response.success) {
                    // 过滤掉下架商品（shelveStatus === 0）
                    const availableFoods = response.data.filter(food => food.shelveStatus === 1);
                    foodArr.value = availableFoods.map(item => ({
                        id: item.id,
                        foodId: item.id,
                        foodName: item.foodName,
                        foodPrice: item.foodPrice,
                        foodExplain: item.foodExplain,
                        foodImg: item.foodImg,
                        remarks: item.remarks,
                        businessId: item.businessId,
                        businessName: item.businessName,
                        stock: item.stock,
                        category: item.category || '其他',
                        purchaseLimit: item.purchaseLimit
                    }));
                } else {
                    const errorMsg = response.message || "获取食品列表失败";
                    console.error("食品列表API返回失败:", errorMsg);
                    throw new Error(errorMsg);
                }
            } catch (error) {
                console.error("获取食品列表失败:", error);
                console.error("错误详情:", error.response || error.message);

                foodArr.value = [];
            } finally {
                loadingFoods.value = false;
            }
        };

        // 跳转到购物车页面
        const goToCart = () => {
            if (totalQuantity.value === 0) {
                toast.error("请先添加商品到购物车");
                return;
            }
            router.push({
                path: "/cart",
                query: {
                    businessId: businessId.value,
                    serviceMode: deliveryMode.value
                }
            });
        };

        // 跳转到订单页面
        const toOrder = () => {
            if (totalQuantity.value === 0) {
                toast.error("购物车为空");
                return;
            }
            // 先进入购物车确认数量与勾选商品，再进入地址/自取确认页。
				router.push({
					path: '/cart',
				query: {
                    businessId: businessId.value,
                    serviceMode: deliveryMode.value,
				}
			});
        };

        // 计算属性 - 基于后端购物车数据
        const totalPrice = computed(() => {
            const total = cartItems.value.reduce((total, item) => {
                return total + (item.foodPrice || 0) * (item.quantity || 0);
            }, 0);
            return total;
        });

        const totalQuantity = computed(() => {
            const quantity = cartItems.value.reduce((sum, item) => sum + (item.quantity || 0), 0);
            return quantity;
        });

        const totalSettle = computed(() => {
            const settle = totalPrice.value + (deliveryMode.value === 'pickup' ? 0 : (business.value.deliveryPrice || 0));
            return settle;
        });

        // 检查是否达到起送费
        const canOrder = computed(() => {
            const canOrder = totalQuantity.value > 0
                && (business.value.status === undefined || business.value.status === 1)
                && business.value.operatingStatus !== false
                && (deliveryMode.value !== 'pickup' || business.value.dineInAvailable !== false)
                && (deliveryMode.value === 'pickup' || totalPrice.value >= Number(business.value.startPrice || 0));
            return canOrder;
        });
        const isBusinessOpen = computed(() => (business.value.status === undefined || business.value.status === 1)
            && business.value.operatingStatus !== false);
        const orderButtonText = computed(() => {
            if (!isBusinessOpen.value) return '休息中';
            if (totalQuantity.value === 0) return '请选择餐品';
            if (deliveryMode.value === 'delivery' && totalPrice.value < Number(business.value.startPrice || 0)) {
                return `还差 ¥${formatMoney(Number(business.value.startPrice || 0) - totalPrice.value)} 起送`;
            }
            return '去结算';
        });

        // 初始化
        onMounted(async () => {
            businessId.value = parseInt(route.query.businessId);

            if (!businessId.value) {
                console.error("无效的商家ID:", route.query.businessId);
                router.push("/");
                return;
            }

            await fetchUserInfo();
            await fetchBusinessInfo();
            await fetchFoodList();
            await fetchCartList(); // 获取购物车数据
            await loadReactions();
        });

        // 监听businessId变化
        watch(() => route.query.businessId, (newId) => {
            if (newId && parseInt(newId) !== businessId.value) {
                businessId.value = parseInt(newId);
                fetchUserInfo();
                fetchBusinessInfo();
                fetchFoodList();
                fetchCartList(); // 重新获取购物车数据
                loadReactions();
                reviews.value = [];
                activeTab.value = 'order';
            }
        });

        return {
            business,
            foodArr,
            activeCategory,
            menuCategories,
            visibleFoods,
            loadingBusiness,
            loadingFoods,
            totalPrice,
            totalQuantity,
            totalSettle,
            canOrder,
            isBusinessOpen,
            orderButtonText,
            isLiked,
            isFavorited,
            getCartQuantity,
            isCartQuantityAtLimit,
            cartQuantityLimitMessage,
            add: addToCart,
            minus: removeFromCart,
            toOrder,
            goToCart,
            toggleLike,
            toggleFavorite,
            interactionLoading,
            activeTab,
            deliveryMode,
            setDeliveryMode,
            selectTab,
            handleImageError,
            focusMenu,
            goBack,
            reviews,
            loadingReviews,
            reviewAverage,
            reviewStars,
            isSoldOut,
            formatDate,
            formatMoney
        };
    }
};
</script>

<style scoped>
.stock-label{font-size:12px;color:#8aa0b2;margin-left:8px}.fa-plus-circle.disabled{color:#b8c6d1;cursor:not-allowed}
/* 样式部分保持不变 */
/****************** 总容器 ******************/
.wrapper {
    width: 100%;
    height: 100%;
}

/****************** header部分 ******************/
/* .wrapper header {
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
} */
.wrapper .header {
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
.wrapper title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color:white;
}
/****************** 商家logo部分 ******************/
.wrapper .business-logo {
    width: 100%;
    height: 50vw;
    /*使用上外边距避开header部分*/
    margin-top: 12vw;
    display: flex;
    justify-content: center;
    align-items: center;
}

.wrapper .business-logo img {
    width: 40vw;
    height: 40vw;
    border-radius: 5px;
}

/****************** 商家信息部分 ******************/
.wrapper .business-info {
    width: 100%;
    height: 20vw;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    position: relative;
}

.wrapper .business-info h1 {
    font-size: 5vw;
}

.wrapper .business-info .reactions {
    position: absolute;
    right: 3vw;
    bottom: 7vw;
    display: flex;
    gap: 4vw;
}

.wrapper .business-info .reactions .reaction {
    display: flex;
    align-items: center;
    gap: 1vw;
    cursor: pointer;
    user-select: none;
}

.wrapper .business-info .reactions .reaction i {
    font-size: 5vw;
    color: #bbb;
}

.wrapper .business-info p {
    font-size: 3vw;
    color: #666;
    margin-top: 1vw;
}

/****************** 食品列表部分 ******************/
.wrapper .food {
    width: 100%;
    /*使用下外边距避开footer部分*/
    margin-bottom: 14vw;
}

.wrapper .food li {
    width: 100%;
    box-sizing: border-box;
    padding: 2.5vw;
    user-select: none;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.wrapper .food li .food-left {
    display: flex;
    align-items: center;
}

.wrapper .food li .food-left img {
    width: 20vw;
    height: 20vw;
}

.wrapper .food li .food-left .food-left-info {
    margin-left: 3vw;
}

.wrapper .food li .food-left .food-left-info h3 {
    font-size: 3.8vw;
    color: #555;
}

.wrapper .food li .food-left .food-left-info p {
    font-size: 3vw;
     font-weight: 600;
    color: #888;
    margin-top: 2vw;
}

.wrapper .food li .food-right {
    width: 16vw;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.wrapper .food li .food-right .fa-minus-circle {
    font-size: 5.5vw;
    color: #999;
    cursor: pointer;
}

.wrapper .food li .food-right p {
    font-size: 3.6vw;
    color: #333;
}

.wrapper .food li .food-right .fa-plus-circle {
    font-size: 5.5vw;
    color: #0097ef;
    cursor: pointer;
}

/****************** 购物车部分 ******************/
.wrapper .cart {
    width: 100%;
    height: 14vw;
    position: fixed;
    left: 0;
    bottom: 0;
    display: flex;
}

.wrapper .cart .cart-left {
    flex: 2;
    background-color: #505051;
    display: flex;
}

.wrapper .cart .cart-left .cart-left-icon {
    width: 16vw;
    height: 16vw;
    box-sizing: border-box;
    border: solid 1.6vw #444;
    border-radius: 8vw;
    background-color: #3190e8;
    font-size: 7vw;
    color: #fff;
    display: flex;
    justify-content: center;
    align-items: center;
    margin-top: -4vw;
    margin-left: 3vw;
    position: relative;
}

.wrapper .cart .cart-left .cart-left-icon-quantity {
    width: 5vw;
    height: 5vw;
    border-radius: 2.5vw;
    background-color: red;
    color: #fff;
    font-size: 3.6vw;
    display: flex;
    justify-content: center;
    align-items: center;
    position: absolute;
    right: -1.5vw;
    top: -1.5vw;
}

.wrapper .cart .cart-left .cart-left-info p:first-child {
    font-size: 4.5vw;
    color: #fff;
    margin-top: 1vw;
}

.wrapper .cart .cart-left .cart-left-info p:last-child {
    font-size: 2.8vw;
    color: #aaa;
}

.wrapper .cart .cart-right {
    flex: 1;
}

/*达到起送费时的样式*/
.wrapper .cart .cart-right .cart-right-item {
    width: 100%;
    height: 100%;
    background-color: #38ca73;
    color: #fff;
    font-size: 4.5vw;
    font-weight: 700;
    user-select: none;
    cursor: pointer;
    display: flex;
    justify-content: center;
    align-items: center;
}

/* 商家详情页：保持简洁的蓝白外卖平台视觉 */
.wrapper {
    min-height: 100vh;
    background: #f5f8fb;
    color: #263f52;
    padding-bottom: 18vw;
}
.store-header {
    height: 58px;
    padding: 0 14px;
    box-sizing: border-box;
    display: flex;
    align-items: center;
    justify-content: flex-start;
    position: sticky;
    top: 0;
    z-index: 10;
    background: #168bd1;
    color: #fff;
}
.back-button,
.header-actions button {
    width: 36px;
    height: 36px;
    border: 0;
    background: transparent;
    color: #fff;
    cursor: pointer;
}
.back-button { font-size: 34px; line-height: 28px; font-family: Arial, sans-serif; }
.header-actions { display: flex; gap: 2px; margin-left: auto; }
.header-actions button { font-size: 18px; }
.header-actions button.active { color: #ffe07d; }
.service-switch {
    display: flex;
    gap: 2px;
    padding: 3px;
    border-radius: 20px;
    background: rgba(255,255,255,.17);
}
.service-switch button {
    min-width: 52px;
    border: 0;
    border-radius: 16px;
    padding: 6px 12px;
    background: transparent;
    color: rgba(255,255,255,.82);
    font-size: 14px;
    cursor: pointer;
}
.service-switch button.active { background: #fff; color: #167fbd; font-weight: 600; }
.store-hero {
    position: relative;
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 18px 16px 20px;
    background: #168bd1;
}
.store-hero .business-logo {
    flex: 0 0 auto;
    width: 78px;
    height: 78px;
    margin: 0;
    object-fit: cover;
    border: 3px solid #fff;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(32,86,120,.16);
    background: #edf5fa;
}
.store-hero .business-info {
    width: auto;
    height: auto;
    min-width: 0;
    align-items: flex-start;
    justify-content: center;
    position: static;
    color: #fff;
}
.business-title-row { display: flex; align-items: center; gap: 8px; }
.store-hero .business-info .business-title-row h1 { color: #fff; font-size: 21px; line-height: 1.3; }
.open-badge { border: 1px solid rgba(255,255,255,.8); border-radius: 4px; padding: 2px 5px; color: #fff; font-size: 11px; }
.open-badge.closed { background:#f2f4f6; border-color:#dce3e9; color:#7d8b98; }
.store-hero .business-info p { margin-top: 6px; color: rgba(255,255,255,.9); font-size: 13px; }
.store-hero .business-info .business-address { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 58vw; }
.business-address i { margin-right: 4px; }
.hero-reactions { position: absolute; right: 14px; bottom: 22px; display: flex; gap: 6px; }
.hero-reactions button { border: 0; background: #fff; color: #7891a2; border-radius: 14px; padding: 5px 8px; font-size: 11px; cursor: pointer; }
.hero-reactions button i { margin-right: 3px; }
.hero-reactions button.active { color: #168bd1; }
.offer-strip { display: flex; gap: 8px; overflow-x: auto; padding: 0 16px 12px; background: #f5f8fb; scrollbar-width: none; }
.offer-strip::-webkit-scrollbar { display: none; }
.offer-strip span { flex: 0 0 auto; border: 1px solid #cfe3f0; border-radius: 4px; background: #fff; color: #4e88aa; padding: 5px 9px; font-size: 12px; }
.page-tabs { display: flex; gap: 28px; padding: 0 18px; background: #fff; border-bottom: 1px solid #e4edf3; }
.page-tabs button { position: relative; border: 0; padding: 14px 1px 12px; background: transparent; color: #7891a2; font-size: 16px; cursor: pointer; }
.page-tabs button.active { color: #168bd1; font-weight: 600; }
.page-tabs button.active::after { content: ''; position: absolute; left: 3px; right: 3px; bottom: -1px; height: 3px; border-radius: 3px 3px 0 0; background: #168bd1; }
.page-tabs small { margin-left: 3px; color: #9db0bd; font-size: 11px; }
.page-content { max-width: 760px; margin: 0 auto; padding: 14px 14px 26px; }
.mode-hint { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; padding: 11px 12px; border: 1px solid #d8e9f3; border-radius: 8px; background: #fff; }
.mode-hint > i { width: 32px; height: 32px; border-radius: 50%; display: grid; place-items: center; background: #e6f3fb; color: #168bd1; }
.mode-hint div { min-width: 0; flex: 1; display: flex; flex-direction: column; gap: 2px; }
.mode-hint strong { color: #315c75; font-size: 14px; }
.mode-hint span { color: #8aa0af; font-size: 12px; }
.mode-hint button { border: 0; background: transparent; color: #168bd1; font-size: 12px; cursor: pointer; }
.section-heading { display: flex; align-items: baseline; justify-content: space-between; margin: 2px 2px 8px; }
.section-heading h2 { color: #2f526a; font-size: 18px; }
.section-heading span { color: #9aadb9; font-size: 12px; }
.state-card { padding: 52px 12px; text-align: center; color: #93a7b4; background: #fff; border: 1px solid #e1edf4; border-radius: 8px; }
.menu-layout { display: grid; grid-template-columns: 108px minmax(0, 1fr); align-items: start; border: 1px solid #e1edf4; border-radius: 8px; overflow: hidden; background: #fff; }
.category-sidebar { display: flex; flex-direction: column; align-self: stretch; background: #eef3f7; }
.category-sidebar button { min-height: 52px; padding: 10px 8px; border: 0; border-bottom: 1px solid #e2e9ee; background: transparent; color: #607b8d; font-size: 13px; line-height: 1.35; cursor: pointer; }
.category-sidebar button.active { position: relative; background: #fff; color: #168bd1; font-weight: 600; }
.category-sidebar button.active::before { content: ''; position: absolute; left: 0; top: 13px; bottom: 13px; width: 3px; border-radius: 0 3px 3px 0; background: #168bd1; }
.category-content { min-width: 0; background: #fff; }
.category-title { padding: 12px 12px 8px; color: #385b72; font-size: 14px; font-weight: 600; border-bottom: 1px solid #eef3f6; }
.wrapper .food { width: auto; margin: 0; padding: 0; background: #fff; border: 1px solid #e1edf4; border-radius: 8px; overflow: hidden; }
.menu-layout .food { border: 0; border-radius: 0; }
.wrapper .food li { width: 100%; min-height: 98px; padding: 13px 12px; box-sizing: border-box; border-bottom: 1px solid #eef3f6; }
.wrapper .food li:last-child { border-bottom: 0; }
.wrapper .food li .food-left img { width: 82px; height: 82px; flex: 0 0 82px; object-fit: cover; border-radius: 7px; background: #f0f5f8; }
.wrapper .food li .food-left .food-left-info { min-width: 0; margin-left: 11px; }
.wrapper .food li .food-left .food-left-info h3 { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #31556d; font-size: 15px; }
.wrapper .food li .food-left .food-left-info p { margin-top: 6px; color: #91a3ae; font-size: 12px; font-weight: 400; }
.wrapper .food li .food-left .food-left-info .food-explain { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 47vw; }
.wrapper .food li .food-left .food-left-info .food-price { display: flex; align-items: baseline; gap: 7px; color: #e76c48; font-size: 16px; font-weight: 600; }
.wrapper .food li .food-left .food-left-info .food-limit { color: #e05252; font-size: 10px; font-weight: 500; white-space: nowrap; }
.sold-out-label { margin-left: 8px; color: #96a8b4; font-size: 11px; font-weight: 400; }
.stock-label { color: #9aacb7; font-size: 11px; font-weight: 400; }
.wrapper .food li .food-right { width: auto; gap: 7px; }
.quantity-btn { width: 27px; height: 27px; padding: 0; border-radius: 50%; border: 1px solid #168bd1; background: #fff; color: #168bd1; font-size: 20px; line-height: 22px; cursor: pointer; }
.quantity-btn.plus-btn { background: #168bd1; color: #fff; }
.quantity-btn.disabled { border-color: #cbd9e1; background: #eef3f6; color: #a9bac5; cursor: not-allowed; }
.quantity { min-width: 16px; text-align: center; color: #496578; font-size: 14px; }
.rating-summary { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; padding: 16px; background: #fff; border: 1px solid #e1edf4; border-radius: 8px; }
.rating-score { color: #e77a45; font-size: 28px; font-weight: 600; }
.rating-stars, .review-stars { color: #f3b34d; letter-spacing: 1px; font-size: 13px; }
.rating-summary span, .rating-count { color: #93a5b2; font-size: 12px; }
.rating-count { margin-left: auto; }
.review-card { padding: 14px; margin-bottom: 10px; background: #fff; border: 1px solid #e1edf4; border-radius: 8px; }
.review-head { display: flex; align-items: center; gap: 8px; }
.review-head strong { color: #385b75; font-size: 14px; }
.review-head time { margin-left: auto; color: #a0b0ba; font-size: 11px; }
.review-card p { margin: 10px 0 0; color: #536d7e; line-height: 1.6; font-size: 13px; }
.merchant-reply { margin-top: 10px; padding: 8px 10px; border-left: 3px solid #168bd1; background: #f2f8fc; color: #5e7b8e; font-size: 12px; line-height: 1.5; }
.story-card { padding: 22px 18px; background: linear-gradient(135deg, #eaf6fd, #fff); border: 1px solid #d7eaf4; border-radius: 8px; }
.story-label { color: #168bd1; font-size: 10px; letter-spacing: 1.5px; }
.story-card h2 { margin-top: 8px; color: #2f526a; font-size: 21px; }
.story-card p { margin-top: 12px; color: #587386; line-height: 1.8; font-size: 14px; }
.store-details { margin-top: 12px; padding: 16px; background: #fff; border: 1px solid #e1edf4; border-radius: 8px; }
.store-details h3 { margin-bottom: 5px; color: #31556d; font-size: 16px; }
.store-details > div { display: flex; gap: 11px; align-items: flex-start; padding: 12px 0; border-bottom: 1px solid #eef3f6; }
.store-details > div:last-child { border-bottom: 0; }
.store-details > div > i { width: 18px; margin-top: 2px; color: #168bd1; text-align: center; }
.store-details > div span { display: flex; flex-direction: column; gap: 4px; }
.store-details b { color: #557286; font-size: 13px; font-weight: 500; }
.store-details em { color: #93a5b2; font-size: 12px; font-style: normal; }
.wrapper .cart { height: 64px; background: #fff; box-shadow: 0 -2px 10px rgba(44,76,96,.12); }
.wrapper .cart .cart-left { flex: 1; background: #fff; color: #31556d; cursor: pointer; }
.wrapper .cart .cart-left .cart-left-icon { width: 49px; height: 49px; margin: -11px 9px 0 14px; border: 0; border-radius: 50%; background: #b6c3ca; font-size: 22px; box-shadow: 0 2px 7px rgba(57,95,114,.22); }
.wrapper .cart .cart-left .cart-left-icon.filled { background: #168bd1; }
.wrapper .cart .cart-left .cart-left-icon-quantity { width: 18px; height: 18px; border-radius: 50%; right: -4px; top: -4px; background: #e85d4a; font-size: 11px; }
.wrapper .cart .cart-left .cart-left-info { min-width: 0; }
.wrapper .cart .cart-left .cart-left-info p:first-child { margin-top: 8px; color: #31556d; font-size: 17px; font-weight: 600; }
.wrapper .cart .cart-left .cart-left-info span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #93a5b2; font-size: 11px; }
.wrapper .cart .cart-right { flex: 0 0 125px; }
.wrapper .cart .cart-right .cart-right-item { height: 64px; width: 100%; border: 0; background: #b8c5cc; color: #fff; font-size: 15px; font-weight: 600; cursor: not-allowed; }
.wrapper .cart .cart-right .cart-right-item.ready { background: #168bd1; cursor: pointer; }
.wrapper .cart .cart-right .cart-right-item:disabled { opacity: 1; }
@media (min-width: 700px) {
    .store-header { padding-left: calc((100% - 760px) / 2 + 14px); padding-right: calc((100% - 760px) / 2 + 14px); }
    .page-content { padding-bottom: 30px; }
    .wrapper { padding-bottom: 0; }
    .wrapper .cart { left: 50%; width: 760px; transform: translateX(-50%); border-radius: 8px 8px 0 0; }
}
@media (max-width: 520px) {
    .page-content { padding-left: 0; padding-right: 0; }
    .mode-hint, .section-heading { margin-left: 12px; margin-right: 12px; }
    .menu-layout { grid-template-columns: 86px minmax(0, 1fr); border-left: 0; border-right: 0; border-radius: 0; }
    .category-sidebar button { min-height: 50px; padding: 9px 6px; font-size: 12px; }
    .wrapper .food li { min-height: 92px; padding: 11px 9px; }
    .wrapper .food li .food-left img { width: 70px; height: 70px; flex-basis: 70px; }
    .wrapper .food li .food-left .food-left-info { margin-left: 9px; }
    .wrapper .food li .food-left .food-left-info h3 { max-width: 32vw; font-size: 14px; }
    .wrapper .food li .food-left .food-left-info .food-explain { max-width: 32vw; }
    .wrapper .food li .food-right { gap: 4px; }
    .quantity-btn { width: 25px; height: 25px; font-size: 18px; }
}
</style>
