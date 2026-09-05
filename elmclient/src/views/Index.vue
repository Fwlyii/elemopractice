<template>
    <!-- 登录、注册部分 -->
    <div class="wrapper">
        <!-- header部分 -->
        <header>
            <div class="icon-location-box">
                <i class="fas fa-map-marker-alt"></i>
            </div>
            <!-- <div class="location-text">天津大学北洋园校区<i class="fa fa-caret-down"></i></div> -->
            <div class="location-text" @click="showLocationPicker">
                <span class="location-display">{{ displayLocation }}</span>
                <i class="fa fa-caret-down"></i>
            </div>

            <!-- 漂亮的位置选择弹窗 -->
            <transition name="fade">
                <div v-if="showPicker" class="location-modal" @click.self="hideLocationPicker">
                    <div class="modal-container">
                        <div class="modal-header">
                            <h3>选择位置</h3>
                            <button class="close-btn" @click="hideLocationPicker">
                                <i class="fa fa-times"></i>
                            </button>
                        </div>

                        <div class="modal-content">
                            <!-- 位置层级导航 -->
                            <div class="location-nav">
                                <div v-for="(level, index) in locationLevels" :key="index"
                                    :class="['nav-item', { active: currentLevel === index, disabled: index > currentLevel }]"
                                    @click="switchLevel(index)">
                                    {{ level }}
                                </div>
                            </div>

                            <!-- 位置列表 -->
                            <div class="location-list-container">
                                <div v-if="loading" class="loading-state">
                                    <i class="fa fa-spinner fa-spin"></i>
                                    <span>加载中...</span>
                                </div>

                                <div v-else-if="locationData.length === 0" class="empty-state">
                                    <i class="fa fa-map-marker"></i>
                                    <span>暂无数据</span>
                                </div>

                                <div v-else class="location-items">
                                    <div v-for="item in locationData" :key="item.id"
                                        :class="['location-item', { selected: isSelected(item) }]"
                                        @click="selectLocation(item)">
                                        <span class="item-name">{{ item.name }}</span>
                                        <i v-if="isSelected(item)" class="fa fa-check selected-icon"></i>
                                    </div>
                                </div>
                            </div>

                            <!-- 当前选择显示 -->
                            <div v-if="selectedLocation.province" class="current-selection">
                                <span>已选择：</span>
                                <span class="selection-text">
                                    {{ getDisplayText(selectedLocation) }}
                                </span>
                            </div>
                        </div>

                        <div class="modal-footer">
                            <button class="btn-cancel" @click="hideLocationPicker">取消</button>
                            <button class="btn-confirm" @click="confirmLocation">确认</button>
                        </div>
                    </div>
                </div>
            </transition>

            <div class="login-register">
                <template v-if="!userInfo">
                    <button @click="goToLChoose">登录</button>
                    <button @click="goToRChoose">注册</button>
                </template>
                <template v-else>
                    <div class="user-info">
                        <div class="scroll-text">
                            <span>{{ userInfo.username }} ，您好！</span>
                        </div>
                    </div>
                </template>
            </div>
        </header>
        <!-- search部分 -->
        <div class="search">
            <div class="search-fixed-top" ref="fixedBox">
                <div class="search-box">
                    <i class="fa fa-search"></i>
                    <input v-model="searchKeyword" type="text" placeholder="搜索饿了么商家" @keyup.enter="performSearch" />
                    <button @click="performSearch" class="search-btn">搜索</button>
                </div>
            </div>
        </div>


        <!-- 点餐分类部分 -->
        <ul class="foodtype">
            <li @click="toBusinessList(1)">
                <img src="@/assets/dcfl01.png" alt="美食">
                <p>美食</p>
            </li>
            <li @click="toBusinessList(2)">
                <img src="@/assets/dcfl02.png" alt="早餐">
                <p>早餐</p>
            </li>
            <li @click="toBusinessList(3)">
                <img src="@/assets/dcfl03.png" alt="跑腿代购">
                <p>跑腿代购</p>
            </li>
            <li @click="toBusinessList(4)">
                <img src="@/assets/dcfl04.png" alt="汉堡披萨">
                <p>汉堡披萨</p>
            </li>
            <li @click="toBusinessList(5)">
                <img src="@/assets/dcfl05.png" alt="甜品饮品">
                <p>甜品饮品</p>
            </li>
            <li @click="toBusinessList(6)">
                <img src="@/assets/dcfl06.png" alt="速食简餐">
                <p>速食简餐</p>
            </li>
            <li @click="toBusinessList(7)">
                <img src="@/assets/dcfl07.png" alt="地方小吃">
                <p>地方小吃</p>
            </li>
            <li @click="toBusinessList(8)">
                <img src="@/assets/dcfl08.png" alt="米粉面馆">
                <p>米粉面馆</p>
            </li>
            <li @click="toBusinessList(9)">
                <img src="@/assets/dcfl09.png" alt="包子粥铺">
                <p>包子粥铺</p>
            </li>
            <li @click="toBusinessList(10)">
                <img src="@/assets/dcfl10.png" alt="炸鸡炸串">
                <p>炸鸡炸串</p>
            </li>
        </ul>

        <!-- 猜你喜欢：保留为轻量横向推荐，不再突出销量冠军或排名 -->
        <section v-if="suggestedBusinesses.length" class="guess-section" aria-label="猜你喜欢">
            <div class="section-heading">
                <div><h2>猜你喜欢</h2><span>附近口碑好店</span></div>
                <button type="button" @click="scrollToRecommendations">更多 <i class="fa fa-angle-right"></i></button>
            </div>
            <div class="guess-scroll">
                <button v-for="business in suggestedBusinesses" :key="business.id || business.businessId" type="button" class="guess-card" @click="toBusinessInfo(business.id || business.businessId)">
                    <img :src="business.businessImg || require('@/assets/business-default.png')" :alt="business.businessName" @error="handleImageError">
                    <strong>{{ business.businessName || '附近好店' }}</strong>
                    <span><b>{{ hasBusinessRating(business.score) ? `★ ${Number(business.score).toFixed(1)}` : '暂无评分' }}</b> · 人均 ¥{{ formatMoney(business.averagePrice || business.avgPrice || 20) }}</span>
                </button>
            </div>
        </section>

        <!-- 推荐商家部分 -->
        <div id="recommendations" class="recommend">
            <div class="recommend-line"></div>
            <p>推荐商家</p>
            <div class="recommend-line"></div>
        </div>

        <!-- 推荐方式部分 -->
        <ul class="recommendtype">
            <li :class="{ active: sortBy === 'default' }" @click="setSortBy('default')">
                综合排序<i class="fa fa-caret-down"></i>
            </li>

            <li :class="{ active: sortBy === 'sales' }" @click="setSortBy('sales')">
                销量最高
            </li>
            <li :class="{ active: showFilter }" @click="toggleFilter">
                筛选<i class="fa fa-filter"></i>
            </li>
        </ul>

        <!-- 筛选弹窗 -->
        <transition name="fade">
            <div v-if="showFilter" class="filter-modal" @click.self="hideFilter">
                <div class="filter-container">
                    <div class="filter-header">
                        <h3>筛选条件</h3>
                        <button class="close-btn" @click="hideFilter">
                            <i class="fa fa-times"></i>
                        </button>
                    </div>

                    <div class="filter-content">
                        <!-- 免配送费筛选 -->
                        <div class="filter-section">
                            <h4>配送费</h4>
                            <label class="filter-option">
                                <input type="checkbox" v-model="filters.freeDelivery" @change="applyFilters">
                                <span>免配送费</span>
                            </label>
                            <label class="filter-option">
                                <input type="checkbox" v-model="filters.promotionOnly" @change="applyFilters">
                                <span>有满减活动</span>
                            </label>
                        </div>

                        <div class="filter-section">
                            <h4>到店方式</h4>
                            <label class="filter-option">
                                <input type="checkbox" v-model="filters.dineIn" @change="applyFilters">
                                <span>支持堂食</span>
                            </label>
                        </div>

                        <!-- 起送价筛选 -->
                        <div class="filter-section">
                            <h4>起送价</h4>
                            <div class="price-range">
                                <label class="filter-option">
                                    <input type="radio" name="startPrice" value="0" v-model="filters.startPrice"
                                        @change="applyFilters">
                                    <span>不限</span>
                                </label>
                                <label class="filter-option">
                                    <input type="radio" name="startPrice" value="20" v-model="filters.startPrice"
                                        @change="applyFilters">
                                    <span>20元以下</span>
                                </label>
                                <label class="filter-option">
                                    <input type="radio" name="startPrice" value="30" v-model="filters.startPrice"
                                        @change="applyFilters">
                                    <span>30元以下</span>
                                </label>
                                <label class="filter-option">
                                    <input type="radio" name="startPrice" value="50" v-model="filters.startPrice"
                                        @change="applyFilters">
                                    <span>50元以下</span>
                                </label>
                            </div>
                        </div>
                    </div>

                    <div class="filter-footer">
                        <button class="btn-reset" @click="resetFilters">重置</button>
                        <button class="btn-confirm" @click="confirmFilters">确定</button>
                    </div>
                </div>
            </div>
        </transition>

        <!-- 推荐商家列表部分 -->
        <div v-if="!businessList || businessList.length === 0" class="empty-business-list">
            <div class="empty-state">
                <i class="fa fa-store"></i>
                <p>暂无商家数据</p>
                <p class="empty-hint">请稍后再试或检查网络连接</p>
            </div>
        </div>

        <ul class="business-list" v-if="businessList && businessList.length > 0">
            <li v-for="business in visibleBusinessList" :key="business.id || business.businessId"
                @click="toBusinessInfo(business.id || business.businessId)">
                <div class="business-info">
                    <img :src="business.businessImg || require('@/assets/business-default.png')"
                        @error="handleImageError" :alt="business.businessName">
                    <div class="business-info-detail">
                        <h3>{{ business.businessName || '未命名商铺'}} <small v-if="business.operatingStatus === false" class="closed-shop-tag">休息中</small></h3>
                        <div class="business-info-rating">
                            <span class="rating-score">{{ formatBusinessRating(business.score) }}</span>
                            <span class="monthly-sales">月售 {{ business.salesCount || 0 }}</span>
                            <span class="average-price">人均 ¥{{ formatMoney(business.averagePrice || business.startPrice || 0) }}</span>
                        </div>
                        <div class="business-info-delivery">
                            <span class="start-price">起送 ¥{{ (business.startPrice || 0).toFixed(2) }}</span>
                            <span class="delivery-fee" :class="{ 'free-delivery': (business.deliveryPrice || 0) === 0 }">
                                {{ (business.deliveryPrice || 0) === 0 ? '免配送费' : `配送 ¥${(business.deliveryPrice || 0).toFixed(2)}` }}
                            </span>
                        </div>
                        <div class="business-tags">
                            <span v-for="tag in getBusinessTags(business)" :key="tag.label" :class="['business-tag', `tag-${tag.tone || 'neutral'}`]">{{ tag.label }}</span>
                        </div>
                    </div>
                </div>
            </li>
        </ul>
        <button v-if="hasMoreBusinesses" type="button" class="load-more" @click="loadMoreBusinesses">加载更多商家</button>
<ai-chatbot class="ai-chat"/>
        <!-- 底部菜单部分 -->

    </div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue';
import AiChatbot from '../components/AiChatbot.vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';
import { formatMoney, formatRating } from '../utils/formatters';
import { clearAuth, getStoredUser, getToken, updateStoredUser } from '../utils/auth';
import { useLocationPicker } from '../composables/useLocationPicker';
import {
    getBusinessTags,
    getRecommendationScore,
    hasConfiguredPromotion,
    supportsDineIn
} from '../utils/businessPresentation';
export default {
    name: 'Index',
    setup() {
        const fixedBox = ref(null);
        const router = useRouter();
        const userInfo = ref(null);
        const businessList = ref([]);
        const originalBusinessList = ref([]); // 保存原始数据用于筛选和排序
        const currentPage = ref(1);
        const pageSize = 6;
        const suggestedBusinesses = computed(() => businessList.value.slice(0, 3));
        const visibleBusinessList = computed(() => businessList.value.slice(0, currentPage.value * pageSize));
        const hasMoreBusinesses = computed(() => visibleBusinessList.value.length < businessList.value.length);
        const {
            displayLocation,
            showPicker,
            loading,
            locationData,
            currentLevel,
            locationLevels,
            selectedLocation,
            showLocationPicker,
            hideLocationPicker,
            switchLevel,
            selectLocation,
            isSelected,
            confirmLocation,
            getDisplayText,
            restoreSavedLocation
        } = useLocationPicker();

        const scrollToRecommendations = () => {
            document.getElementById('recommendations')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        };

        const searchKeyword = ref('');
        const sortBy = ref('default');
        const showFilter = ref(false);
        const filters = ref({
            freeDelivery: false,
            startPrice: '0',
            promotionOnly: false,
            dineIn: false
        });
        const fetchUserInfo = async () => {
            const storedUser = getStoredUser();
            if (storedUser?.id && storedUser?.username) {
                userInfo.value = storedUser;
                return;
            }
            if (!getToken()) return;

            try {
                const res = await request.get('/api/user');
                if (res && res.id && res.username) {
                    userInfo.value = res;
                    updateStoredUser(res);
                } else {
                    console.error('获取用户信息失败：接口返回数据不完整');
                    userInfo.value = null;
                }
            } catch (error) {
                console.error('获取用户信息异常:', error);
                userInfo.value = null;
                if (error.response?.status === 401) clearAuth();
            }
        };

        const hasBusinessRating = (score) => formatRating(score) !== null;
        const numericBusinessRating = (score) => Number(formatRating(score) || 0);
        const formatBusinessRating = (score) => formatRating(score) ? `${formatRating(score)}分` : '暂无评分';

        // 排序商家列表
        const sortBusinessList = (list, sortType) => {
            const sortedList = [...list];

            switch (sortType) {
                case 'default':
                    // 综合排序：按“个性化标签 > 评分 > 订单量 > 新鲜度”排序。
                    // 标签本身由 getBusinessTags 的阈值规则产生，避免首页只按销量粗暴排序。
                    sortedList.sort((a, b) => {
                        const recommendationDiff = getRecommendationScore(b) - getRecommendationScore(a);
                        if (Math.abs(recommendationDiff) > 0.01) return recommendationDiff;
                        const scoreA = numericBusinessRating(a.score);
                        const scoreB = numericBusinessRating(b.score);

                        // 评分比较（保留一位小数精度）
                        const scoreDiff = Math.round((scoreB - scoreA) * 10) / 10;
                        if (Math.abs(scoreDiff) >= 0.1) {
                            return scoreDiff; // 评分降序
                        }

                        // 评分相同，按ID排序（ID越大表示越新）
                        const idA = parseInt(a.id || a.businessId || 0);
                        const idB = parseInt(b.id || b.businessId || 0);
                        return idB - idA; // ID降序（新的在前）
                    });
                    break;

                case 'sales':
                    // 销量排序：销量优先，销量相同则按ID排序（ID大的在前）
                    sortedList.sort((a, b) => {
                        const salesA = parseInt(a.salesCount || 0);
                        const salesB = parseInt(b.salesCount || 0);
                        if (salesA !== salesB) return salesB - salesA;

                        // 销量相同，按ID排序（ID越大表示越新）
                        const idA = parseInt(a.id || a.businessId || 0);
                        const idB = parseInt(b.id || b.businessId || 0);
                        return idB - idA; // ID降序（新的在前）
                    });
                    break;

                default:
                    // 默认不排序，保持原有顺序
                    break;
            }

            return sortedList;
        };

        const navigateToOrders = () => {
            router.push({ path: '/orderList' });
        };
        const handleScroll = () => {
            let scroll = window.scrollY || document.documentElement.scrollTop;
            let width = document.documentElement.clientWidth;
            let search = fixedBox.value;

            if (scroll > width * 0.12) {
                search.style.position = 'fixed';
                search.style.left = '0';
                search.style.top = '0';
            } else {
                search.style.position = 'static';
            }
        };
        onMounted(() => {
            restoreSavedLocation();
            // 加载用户信息
            fetchUserInfo();

            window.addEventListener('scroll', handleScroll);

            getBusinessList();
        });

        onBeforeUnmount(() => {
            window.removeEventListener('scroll', handleScroll);
        });

        const toBusinessList = (orderTypeId) => {
            router.push({ path: '/BusinessList', query: { orderTypeId } });
        };
        const goToLChoose = () => {
            // 跳转到登录页面
            router.push({ path: '/login', query: { role: 'user' } });
        };
        const goToRChoose = () => {
            router.push({ path: '/register' });
        }
        const navigateToSearch = () => {
            router.push({ path: '/search' });
        };

        // 执行搜索
        const performSearch = async () => {
            if (searchKeyword.value.trim() !== '') {
                try {
                    const params = {
                        keyword: searchKeyword.value.trim()
                    };

                    // 根据排序方式添加参数
                    if (sortBy.value === 'score') {
                        params.isScore = 1;
                        params.isSales = 0;
                    } else if (sortBy.value === 'sales') {
                        params.isScore = 0;
                        params.isSales = 1;
                    } else {
                        params.isScore = 0;
                        params.isSales = 0;
                    }

                    const response = await request.get('/api/businesses/search', { params });
                    const searchData = response?.success && Array.isArray(response.data)
                        ? response.data
                        : (Array.isArray(response) ? response : []);
                    originalBusinessList.value = searchData;
                    applyFiltersAndSort();
                } catch (error) {
                    console.error('搜索失败:', error);
                    getBusinessList();
                }
            } else {
                getBusinessList();
            }
        };

        // 设置排序方式
        const setSortBy = (type) => {
            sortBy.value = type;
            if (searchKeyword.value.trim() !== '') {
                performSearch();
            } else {
                applyFiltersAndSort();
            }
        };

        // 获取商家列表
        const getBusinessList = async () => {
            try {
                const response = await request.get('/api/businesses/search', {
                    params: { keyword: '', isScore: 0, isSales: 0 }
                });
                const businessData = response?.success && Array.isArray(response.data)
                    ? response.data
                    : (Array.isArray(response) ? response : []);
                originalBusinessList.value = businessData;
                applyFiltersAndSort();
            } catch (error) {
                console.error('获取商家列表失败:', error);
                originalBusinessList.value = [];
                businessList.value = [];
            }
        };

        // 处理图片加载失败
        const handleImageError = (e) => {
            e.target.src = require('@/assets/default-business.png');
        };

        // 跳转到商家详情页
        const toBusinessInfo = (businessId) => {
            router.push({
                path: '/businessInfo',
                query: { businessId }
            });
        };

        // 筛选功能
        const toggleFilter = () => {
            showFilter.value = !showFilter.value;
        };

        const hideFilter = () => {
            showFilter.value = false;
        };

        // 应用筛选和排序的统一函数
        const applyFiltersAndSort = () => {
            let filteredList = [...originalBusinessList.value];

            // 免配送费筛选
            if (filters.value.freeDelivery) {
                filteredList = filteredList.filter(business =>
                    business.deliveryPrice === 0 || business.deliveryPrice === null
                );
            }

            if (filters.value.promotionOnly) {
                filteredList = filteredList.filter(hasConfiguredPromotion);
            }

            if (filters.value.dineIn) {
                filteredList = filteredList.filter(supportsDineIn);
            }

            // 起送价筛选
            if (filters.value.startPrice !== '0') {
                const maxPrice = parseInt(filters.value.startPrice);
                filteredList = filteredList.filter(business => {
                    const startPrice = business.startPrice || business.starPrice || 0;
                    return startPrice <= maxPrice;
                });
            }

            businessList.value = sortBusinessList(filteredList, sortBy.value);
            currentPage.value = 1;
        };

        const applyFilters = () => {
            applyFiltersAndSort();
        };

        const resetFilters = () => {
            filters.value = {
                freeDelivery: false,
                startPrice: '0',
                promotionOnly: false,
                dineIn: false
            };
            sortBy.value = 'default'; // 重置排序为默认
            applyFiltersAndSort();
        };

        const confirmFilters = () => {
            applyFiltersAndSort();
            hideFilter();
        };

        const loadMoreBusinesses = () => {
            if (hasMoreBusinesses.value) currentPage.value += 1;
        };

        return {
            fixedBox,
            toBusinessList,
            navigateToOrders,
            goToLChoose,
            goToRChoose,
            userInfo,
            isuser: computed(() => !!userInfo.value),
            navigateToSearch,
            businessList,
            visibleBusinessList,
            hasMoreBusinesses,
            loadMoreBusinesses,
            toBusinessInfo,
            handleImageError,
            formatBusinessRating,
            hasBusinessRating,
            displayLocation,
            showPicker,
            loading,
            locationData,
            currentLevel,
            locationLevels,
            selectedLocation,
            showLocationPicker,
            hideLocationPicker,
            switchLevel,
            selectLocation,
            isSelected,
            confirmLocation,
            getDisplayText,
            searchKeyword,
            sortBy,
            performSearch,
            setSortBy,
            showFilter,
            filters,
            toggleFilter,
            hideFilter,
            applyFilters,
            resetFilters,
            confirmFilters,
            applyFiltersAndSort,
            sortBusinessList,
            suggestedBusinesses,
            formatMoney,
            getBusinessTags,
            getRecommendationScore,
            scrollToRecommendations
        };
    },
    components: {
        AiChatbot
    }
}
</script>

<style scoped>
/****************** 总容器 ******************/
.wrapper {
    width: 100%;
    height: 100%;
}

/****************** header ******************/
.wrapper header {
    width: 100%;
    height: 12vw;
    background-color: #0097ff;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 4vw;
    box-sizing: border-box;
}

/* 确保位置信息不会被挤压 */
.wrapper header .icon-location-box {
    width: 3.5vw;
    height: 3.5vw;
    margin-right: 1vw;
    flex-shrink: 0;
}

.wrapper header .location-text {
    font-size: 4.5vw;
    font-weight: 700;
    color: #fff;
    flex-shrink: 0;
    white-space: nowrap;
}

.wrapper header .icon-location-box i {
    font-size: 5vw;
    color: #fff;
}

.wrapper header .location-text .fa-caret-down {
    margin-left: 1vw;
}

.user-info {
    width: 150px;
    /* 你可以根据右上角区域宽度调整 */
    overflow: hidden;
    white-space: nowrap;
    position: relative;
}

.scroll-text {
    display: inline-block;
    padding-left: 100%;
    /* 给动画留出空白 */
    animation: scroll-text 10s linear infinite;
}

@keyframes scroll-text {
    0% {
        transform: translateX(0);
    }

    100% {
        transform: translateX(-100%);
    }
}


/****************** 登录、注册部分 ******************/
.wrapper .login-register {
    display: flex;
    gap: 2vw;
    align-items: center;
    margin-left: 5vw;
    flex-grow: 1;
    justify-content: flex-end;
    /* 关键修改：此属性是解决 Flexbox 布局中子元素溢出问题的关键 */
    min-width: 0;
}

.wrapper .login-register .user-info {
    /* 删除 max-width: 100%，以确保容器可以根据内容宽度进行溢出 */
    font-size: 4vw;
    font-weight: 500;
    color: #fff;
    white-space: nowrap;
    /* 强制文本不换行 */

    /* 核心修改：允许水平滚动 */
    overflow-x: auto;
    /* 在水平方向上允许滚动 */
    overflow-y: hidden;
    /* 隐藏垂直方向的滚动条 */
    -webkit-overflow-scrolling: touch;
    /* 针对 iOS 设备实现更流畅的滚动 */

    /* 隐藏滚动条但保留滚动功能，让界面更美观 */
    scrollbar-width: none;
    /* 针对 Firefox */
    -ms-overflow-style: none;
    /* 针对 Internet Explorer 和 Edge */
}

/* 针对 Chrome, Safari 等 Webkit 内核浏览器隐藏滚动条 */
.wrapper .login-register .user-info::-webkit-scrollbar {
    display: none;
}

.wrapper .login-register button {
    padding: 1.5vw 3vw;
    border: none;
    background-color: white;
    color: #0097ff;
    cursor: pointer;
    border-radius: 1vw;
    transition: background-color 0.3s;
    font-size: 3.5vw;
    flex-shrink: 0;
}

.wrapper .login-register button:hover {
    background-color: #f0f0f0;
}

/****************** search ******************/
.wrapper .search {
    width: 100%;
    height: 13vw;
}

.wrapper .search .search-fixed-top {
    width: 100%;
    height: 13vw;
    background-color: #0097FF;
    display: flex;
    justify-content: center;
    align-items: center;
    position: relative;
    z-index: 20;
    /* 确保搜索框在轮播图之上 */
}

.wrapper .search .search-fixed-top .search-box {
    width: 90%;
    height: 9vw;
    background-color: #fff;
    border-radius: 2px;

    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 2vw;

    font-size: 3.5vw;
    color: #AEAEAE;
    font-family: "宋体";
    /*此样式是让文本选中状态无效*/
    user-select: none;
}

.wrapper .search .search-fixed-top .search-box input {
    flex: 1;
    border: none;
    outline: none;
    background: transparent;
    font-size: 3.5vw;
    color: #333;
    margin: 0 1vw;
}

.wrapper .search .search-fixed-top .search-box input::placeholder {
    color: #AEAEAE;
}

.wrapper .search .search-fixed-top .search-box .search-btn {
    background: #0097ff;
    color: white;
    border: none;
    padding: 1.5vw 3vw;
    border-radius: 1vw;
    font-size: 3vw;
    cursor: pointer;
    transition: background-color 0.3s;
}

.wrapper .search .search-fixed-top .search-box .search-btn:hover {
    background: #0080e0;
}

.wrapper .search .search-fixed-top .search-box .fa-search {
    margin-right: 1vw;
}

/* 排序选项样式 */
.sort-options {
    width: 100%;
    padding: 3vw;
    background-color: #f8f9fa;
    border-bottom: 1px solid #e0e0e0;
}

.sort-buttons {
    display: flex;
    gap: 2vw;
    justify-content: center;
    flex-wrap: wrap;
}

.sort-buttons button {
    padding: 2vw 4vw;
    border: 1px solid #ddd;
    background-color: white;
    color: #666;
    border-radius: 2vw;
    cursor: pointer;
    transition: all 0.3s;
    font-size: 3.2vw;
    min-width: 20vw;
}

.sort-buttons button:hover {
    border-color: #0097ff;
    color: #0097ff;
}

.sort-buttons button.active {
    background-color: #0097ff;
    color: white;
    border-color: #0097ff;
}

/****************** 点餐分类部分 ******************/
.wrapper .foodtype {
    width: 100%;
    height: 48vw;
    background-color: white;
    display: flex;
    flex-wrap: wrap;
    justify-content: space-around;
    /*要使用align-content。10个子元素将自动换行为两行，而且两行作为一个整体垂直居中*/
    align-content: center;
}

.wrapper .foodtype li {
    /*一共10个子元素，通过计算，子元素宽度在16.7 ~ 20 之间，才能保证换两行*/
    width: 18vw;
    height: 20vw;

    display: flex;
    /*弹性盒子主轴方向设为column，然后仍然是垂直水平方向居中*/
    flex-direction: column;
    justify-content: center;
    align-items: center;

    user-select: none;
    cursor: pointer;
}

.wrapper .foodtype li img {
    width: 12vw;
    /*视频讲解时高度设置为12vw，实际上设置为10.3vw更佳*/
    height: 10.3vw;
}

.wrapper .foodtype li p {
    font-size: 3.2vw;
    color: #666;
}

/****************** 销量冠军3D轮播图部分 ******************/
.wrapper .top-businesses-carousel {
    width: 95%;
    margin: 1.5vw auto;
    /* 进一步减少上下外边距 */
    background: white;
    border-radius: 2vw;
    padding: 1.5vw 2vw;
    /* 进一步减少上下内边距 */
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    position: relative;
    z-index: 10;
    /* 确保在搜索框之下 */
}

.wrapper .top-businesses-carousel .carousel-header {
    text-align: center;
    margin-bottom: 1.5vw;
    /* 减少标题下方间距 */
    color: #333;
}

.wrapper .top-businesses-carousel .carousel-header h3 {
    font-size: 6vw;
    /* 增大字体大小 */
    margin: 0 0 1vw 0;
    font-weight: 700;
    text-shadow: none;
}

.wrapper .top-businesses-carousel .carousel-header p {
    font-size: 2.8vw;
    margin: 0;
    opacity: 0.7;
    color: #666;
}

.wrapper .top-businesses-carousel .carousel-3d-container {
    position: relative;
    height: 50vw;
    /* 减少整体高度，让占位更小 */
    min-height: 320px;
    /* 减少最小高度 */
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 3vw 12vw;
    /* 减少内边距，让占位更小 */
    margin: -3vw -12vw;
    /* 调整负边距 */
}

.wrapper .top-businesses-carousel .carousel-3d-item {
    position: absolute;
    transition: all 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
    cursor: pointer;
    transform-style: preserve-3d;
}

/* 中间激活状态 */
.wrapper .top-businesses-carousel .carousel-3d-item.active {
    z-index: 13;
    transform: translateX(0) scale(1);
    opacity: 1;
}

/* 左边状态 */
.wrapper .top-businesses-carousel .carousel-3d-item.left {
    z-index: 12;
    transform: translateX(-20vw) scale(0.75);
    opacity: 0.6;
}

/* 右边状态 */
.wrapper .top-businesses-carousel .carousel-3d-item.right {
    z-index: 12;
    transform: translateX(20vw) scale(0.75);
    opacity: 0.6;
}

.wrapper .top-businesses-carousel .business-card-3d {
    width: 38vw;
    /* 减少卡片宽度 */
    min-width: 240px;
    /* 减少最小宽度 */
    background: white;
    border-radius: 2vw;
    padding: 2.5vw;
    /* 减少内边距 */
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
    position: relative;
    overflow: hidden;
}

.wrapper .top-businesses-carousel .carousel-3d-item:hover .business-card-3d {
    transform: translateY(-0.5vw);
    box-shadow: 0 6px 24px rgba(0, 0, 0, 0.2);
}

.wrapper .top-businesses-carousel .rank-badge {
    position: absolute;
    top: 0;
    right: 0;
    padding: 1.5vw 3vw;
    border-radius: 0 2vw 0 2vw;
    color: white;
    font-weight: 700;
    font-size: 2.5vw;
    z-index: 15;
}

.wrapper .top-businesses-carousel .rank-badge.champion {
    background: #1d8bd1;
}

.wrapper .top-businesses-carousel .rank-badge.runner-up {
    background: #4d9fcf;
}

.wrapper .top-businesses-carousel .rank-badge.third {
    background: #76b4d8;
}

.wrapper .top-businesses-carousel .business-image {
    width: 100%;
    height: 25vw;
    /* 调整图片高度，在更小的卡片中保持比例 */
    min-height: 160px;
    /* 调整最小高度 */
    border-radius: 1.5vw;
    overflow: hidden;
    margin-bottom: 1.5vw;
    /* 减少底部间距 */
}

.wrapper .top-businesses-carousel .business-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.3s ease;
}

.wrapper .top-businesses-carousel .carousel-3d-item:hover .business-image img {
    transform: scale(1.05);
}

.wrapper .top-businesses-carousel .business-info {
    text-align: center;
}

.wrapper .top-businesses-carousel .business-info h4 {
    font-size: 3.2vw;
    /* 稍微减少字体大小 */
    font-weight: 700;
    color: #333;
    margin: 0 0 1vw 0;
    /* 减少底部间距 */
    line-height: 1.2;
}

.wrapper .top-businesses-carousel .stats {
    display: flex;
    justify-content: space-between;
    margin-bottom: 1.5vw;
    gap: 1.5vw;
}

.wrapper .top-businesses-carousel .stat-item {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.8vw;
    padding: 1.2vw 2vw;
    border-radius: 2vw;
    font-size: 2.5vw;
    font-weight: 600;
    flex: 1;
    color: white;
}

.wrapper .top-businesses-carousel .rating-stat {
    background: #eaf5ff;
    color: #24577e;
}

.wrapper .top-businesses-carousel .rating-stat .fa-star {
    color: #2588c9;
}

.wrapper .top-businesses-carousel .sales-stat {
    background: #edf7fb;
    color: #24577e;
}

.wrapper .top-businesses-carousel .sales-stat .fa-fire {
    color: #2588c9;
}

.wrapper .top-businesses-carousel .delivery-info {
    display: flex;
    justify-content: space-between;
    gap: 1vw;
}

.wrapper .top-businesses-carousel .delivery-tag {
    display: flex;
    align-items: center;
    gap: 0.5vw;
    background: #f8f9fa;
    padding: 1vw 1.5vw;
    border-radius: 1.5vw;
    border: 1px solid #e9ecef;
    flex: 1;
    justify-content: center;
}

.wrapper .top-businesses-carousel .delivery-tag .tag-label {
    font-size: 2.2vw;
    color: #6c757d;
    font-weight: 500;
}

.wrapper .top-businesses-carousel .delivery-tag .tag-price {
    font-size: 2.4vw;
    color: #007bff;
    font-weight: 600;
}

/* 轮播箭头按钮 */
.wrapper .top-businesses-carousel .carousel-arrow {
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    width: 8vw;
    height: 8vw;
    background: rgba(255, 255, 255, 0.9);
    border: 1px solid #ddd;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    font-size: 3vw;
    color: #333;
    transition: all 0.3s ease;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    z-index: 16;
    opacity: 0;
    visibility: hidden;
}

.wrapper .top-businesses-carousel:hover .carousel-arrow {
    opacity: 1;
    visibility: visible;
}

.wrapper .top-businesses-carousel .carousel-arrow:hover {
    background: white;
    transform: translateY(-50%) scale(1.1);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.wrapper .top-businesses-carousel .carousel-arrow-left {
    left: 8vw;
    /* 调整到扩大的悬停区域内 */
}

.wrapper .top-businesses-carousel .carousel-arrow-right {
    right: 8vw;
    /* 调整到扩大的悬停区域内 */
}

.wrapper .top-businesses-carousel .carousel-indicators {
    display: flex;
    justify-content: center;
    gap: 1.5vw;
    margin-top: 1.5vw;
    /* 减少指示器上方间距 */
}

.wrapper .top-businesses-carousel .indicator {
    width: 2.5vw;
    height: 2.5vw;
    border-radius: 50%;
    background: #ddd;
    cursor: pointer;
    transition: all 0.3s ease;
}

.wrapper .top-businesses-carousel .indicator.active {
    background: #0097ff;
    transform: scale(1.2);
}

/****************** 超级会员部分 ******************/
.wrapper .supermember {
    /*这里也设置容器宽度95%，不能用padding，因为背景色也会充满padding*/
    width: 95%;
    margin: 0 auto;
    height: 11.5vw;
    background-color: #FEEDC1;
    margin-top: 1.3vw;
    border-radius: 2px;
    color: #644F1B;

    display: flex;
    justify-content: space-between;
    align-items: center;
}

.wrapper .supermember .left {
    display: flex;
    align-items: center;
    margin-left: 4vw;
    user-select: none;
}

.wrapper .supermember .left img {
    width: 6vw;
    height: 6vw;
    margin-right: 2vw;
}

.wrapper .supermember .left h3 {
    font-size: 4vw;
    margin-right: 2vw;
}

.wrapper .supermember .left p {
    font-size: 3vw;
}

.wrapper .supermember .right {
    font-size: 3vw;
    margin-right: 4vw;
    cursor: pointer;
}

/****************** 推荐商家部分 ******************/
.wrapper .recommend {
    width: 100%;
    height: 14vw;
    display: flex;
    justify-content: center;
    align-items: center;
}

.wrapper .recommend .recommend-line {
    width: 6vw;
    height: 0.2vw;
    background-color: #888;
}

.wrapper .recommend p {
    font-size: 4vw;
    margin: 0 4vw;
}

/****************** 推荐方式部分 ******************/
.wrapper .recommendtype {
    width: 100%;
    height: 5vw;
    margin-bottom: 5vw;

    display: flex;
    justify-content: space-around;
    align-items: center;
}

.wrapper .recommendtype li {
    font-size: 3.5vw;
    color: #555;
}

/****************** 推荐商家列表部分 ******************/
.wrapper .business-list {
    width: 100%;
    padding: 0;
    margin: 0 0 15vh 0;
    /* 添加底部边距，避免被 Footer 遮挡 */
    list-style: none;
}

.wrapper .business-list li {
    padding: 3vw;
    border-bottom: 1px solid #f0f0f0;
    cursor: pointer;
    transition: background-color 0.3s;
}

.wrapper .business-list li:hover {
    background-color: #f9f9f9;
}

.wrapper .business-list li .business-info {
    display: flex;
    align-items: flex-start;
}

.wrapper .business-list li .business-info img {
    width: 20vw;
    height: 20vw;
    object-fit: cover;
    border-radius: 4px;
}

.wrapper .business-list li .business-info .business-info-detail {
    flex: 1;
    margin-left: 3vw;
}

.wrapper .business-list li .business-info .business-info-detail h3 {
    font-size: 4vw;
    margin: 0 0 2vw 0;
    color: #333;
}

.wrapper .business-list li .business-info .business-info-rating {
    display: flex;
    align-items: center;
    gap: 3vw;
    margin-bottom: 2vw;
}

.wrapper .business-list li .business-info .business-info-rating .rating-score {
    font-size: 3.2vw;
    color: #FF6600;
    font-weight: 600;
}

.wrapper .business-list li .business-info .business-info-rating .monthly-sales {
    font-size: 2.8vw;
    color: #999;
}

.wrapper .business-list li .business-info .business-info-delivery {
    display: flex;
    gap: 2vw;
    margin-bottom: 2vw;
}

.wrapper .business-list li .business-info .business-info-delivery .start-price {
    font-size: 2.8vw;
    color: #666;
}

.wrapper .business-list li .business-info .business-info-delivery .delivery-fee {
    font-size: 2.8vw;
    color: #666;
}

.wrapper .business-list li .business-info .business-info-delivery .delivery-fee.free-delivery {
    color: #FF6600;
    font-weight: 500;
}

.wrapper .business-list li .business-info .business-info-promotion {
    display: flex;
    align-items: center;
}

.wrapper .business-list li .business-info .business-info-promotion .business-info-promotion-left {
    display: flex;
    align-items: center;
    gap: 1vw;
}

.wrapper .business-list li .business-info .business-info-promotion .business-info-promotion-left .business-info-promotion-left-incon {
    background-color: #ff4444;
    color: white;
    padding: 0.5vw 1vw;
    border-radius: 2px;
    font-size: 2.5vw;
}

.wrapper .business-list li .business-info .business-info-promotion .business-info-promotion-left p {
    color: #666;
    font-size: 3vw;
    margin: 0;
}

/* 位置显示样式 */
.location-text {
    cursor: pointer;
    transition: color 0.3s;
    display: flex;
    align-items: center;
    gap: 4px;
}

.location-text:hover {
    color: #e0e0e0;
}

.location-display {
    max-width: 180px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

/* 模态框样式 */
.location-modal {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
    padding: 20px;
}

.modal-container {
    background: white;
    border-radius: 12px;
    width: 100%;
    max-width: 400px;
    max-height: 80vh;
    display: flex;
    flex-direction: column;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
    overflow: hidden;
}

.modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px;
    border-bottom: 1px solid #f0f0f0;
    background: #0097ff;
    color: white;
}

.modal-header h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
}

.close-btn {
    background: none;
    border: none;
    color: white;
    font-size: 20px;
    cursor: pointer;
    padding: 5px;
    border-radius: 50%;
    transition: background-color 0.3s;
}

.close-btn:hover {
    background-color: rgba(255, 255, 255, 0.2);
}

.modal-content {
    display: flex;
    flex-direction: column;
    padding: 20px;
    margin-left: 27px;
    margin-top: 10px;
    margin-bottom: 10px;
    overflow-y: auto;
}

/* 位置导航样式 */
.location-nav {
    display: flex;
    margin-bottom: 20px;
    border-bottom: 2px solid #f0f0f0;
}

.nav-item {
    padding: 12px 20px;
    cursor: pointer;
    border-bottom: 3px solid transparent;
    transition: all 0.3s;
    font-weight: 500;
    color: #666;
}

.nav-item.active {
    color: #0097ff;
    border-bottom-color: #0097ff;
}

.nav-item.disabled {
    color: #ccc;
    cursor: not-allowed;
}

.nav-item:not(.disabled):hover {
    color: #0097ff;
}

/* 位置列表样式 */
.location-list-container {
    min-height: 200px;
    max-height: 300px;
    overflow-y: auto;
    margin-bottom: 20px;
}

.loading-state,
.empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 150px;
    color: #999;
}

.loading-state i,
.empty-state i {
    font-size: 24px;
    margin-bottom: 10px;
}

.location-items {
    display: grid;
    gap: 8px;
}

.location-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s;
}

.location-item:hover {
    border-color: #0097ff;
    background-color: #f8f9ff;
}

.location-item.selected {
    border-color: #0097ff;
    background-color: #e6f3ff;
}

.item-name {
    font-weight: 500;
}

.selected-icon {
    color: #0097ff;
    font-size: 14px;
}

/* 当前选择显示 */
.current-selection {
    padding: 15px;
    background-color: #f8f9fa;
    border-radius: 8px;
    margin-top: 15px;
}

.selection-text {
    font-weight: 600;
    color: #0097ff;
    display: inline-block;
    max-width: 250px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

/* 模态框底部 */
.modal-footer {
    display: flex;
    gap: 12px;
    padding: 20px;
    border-top: 1px solid #f0f0f0;
    background-color: #fafafa;
}

.btn-cancel,
.btn-confirm {
    flex: 1;
    padding: 12px;
    border: none;
    border-radius: 6px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s;
}

.btn-cancel {
    background-color: #f8f9fa;
    color: #666;
}

.btn-cancel:hover {
    background-color: #e9ecef;
}

.btn-confirm {
    background: #0097ff;
    color: white;
}

.btn-confirm:hover {
    background: #087dcc;
    transform: translateY(-1px);
}

/* 动画效果 */
.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}

/* 空状态样式 */
.empty-carousel,
.empty-business-list {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 200px;
    padding: 40px 20px;
}

.empty-carousel .empty-state,
.empty-business-list .empty-state {
    text-align: center;
    color: #999;
}

.empty-carousel .empty-state i,
.empty-business-list .empty-state i {
    font-size: 48px;
    margin-bottom: 16px;
    opacity: 0.5;
}

.empty-carousel .empty-state p,
.empty-business-list .empty-state p {
    font-size: 16px;
    margin: 8px 0;
}

.empty-business-list .empty-state .empty-hint {
    font-size: 14px;
    color: #ccc;
}

/* 推荐方式样式 */
.wrapper .recommendtype {
    width: 100%;
    height: 5vw;
    margin-bottom: 5vw;
    display: flex;
    justify-content: space-around;
    align-items: center;
}

.wrapper .recommendtype li {
    font-size: 3.5vw;
    color: #555;
    cursor: pointer;
    transition: color 0.3s;
    padding: 1vw 2vw;
    border-radius: 1vw;
}

.wrapper .recommendtype li:hover {
    color: #0097ff;
}

.wrapper .recommendtype li.active {
    color: #0097ff;
    background-color: #f0f8ff;
}

/* 筛选弹窗样式 */
.filter-modal {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
    padding: 20px;
}

.filter-container {
    background: white;
    border-radius: 12px;
    width: 100%;
    max-width: 400px;
    max-height: 80vh;
    display: flex;
    flex-direction: column;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
    overflow: hidden;
}

.filter-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px;
    border-bottom: 1px solid #f0f0f0;
    background: #0097ff;
    color: white;
}

.filter-header h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
}

.close-btn {
    background: none;
    border: none;
    color: white;
    font-size: 20px;
    cursor: pointer;
    padding: 5px;
    border-radius: 50%;
    transition: background-color 0.3s;
}

.close-btn:hover {
    background-color: rgba(255, 255, 255, 0.2);
}

.filter-content {
    flex: 1;
    padding: 20px;
    overflow-y: auto;
}

.filter-section {
    margin-bottom: 25px;
}

.filter-section h4 {
    font-size: 16px;
    font-weight: 600;
    margin: 0 0 15px 0;
    color: #333;
}

.filter-option {
    display: flex;
    align-items: center;
    margin-bottom: 12px;
    cursor: pointer;
    font-size: 14px;
    color: #666;
}

.filter-option input[type="checkbox"],
.filter-option input[type="radio"] {
    margin-right: 10px;
    transform: scale(1.2);
}

.filter-option:hover {
    color: #0097ff;
}

.price-range {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.filter-footer {
    display: flex;
    gap: 12px;
    padding: 20px;
    border-top: 1px solid #f0f0f0;
    background-color: #fafafa;
}

.btn-reset,
.btn-confirm {
    flex: 1;
    padding: 12px;
    border: none;
    border-radius: 6px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s;
}

.btn-reset {
    background-color: #f8f9fa;
    color: #666;
}

.btn-reset:hover {
    background-color: #e9ecef;
}

.btn-confirm {
    background: #0097ff;
    color: white;
}

.btn-confirm:hover {
    background: #087dcc;
    transform: translateY(-1px);
}

.ai-chat {
    position: fixed;
  bottom: 100px; /* 距离底部40px */
  right: 20px; /* 距离右边20px */
  z-index: 9999;
}

/* 首页移动端可读性兜底：限制横向内容，避免用户问候和历史样式撑破页面 */
.wrapper { max-width: 600px; margin: 0 auto; overflow-x: hidden; }
.wrapper header { min-width: 0; }
.wrapper header .location-text { min-width: 0; max-width: 48%; font-size: 16px; }
.wrapper header .location-display { display: block; max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.wrapper .login-register { min-width: 0; margin-left: 8px; }
.wrapper .login-register .user-info { width: 104px; max-width: 104px; font-size: 13px; overflow: hidden; }
.wrapper .login-register .scroll-text { display: block; padding-left: 0; animation: none; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.wrapper .top-businesses-carousel { width: calc(100% - 20px); max-width: 560px; overflow: hidden; box-sizing: border-box; }
.wrapper .top-businesses-carousel .carousel-3d-container { width: 100%; margin: 0; padding: 12px 42px; overflow: hidden; }
.wrapper .top-businesses-carousel .business-card-3d { width: min(70vw, 260px); min-width: 0; }
@media (max-width: 480px) {
    .wrapper header .location-text { max-width: 46%; font-size: 15px; }
    .wrapper .login-register .user-info { width: 96px; max-width: 96px; }
    .wrapper .top-businesses-carousel .carousel-3d-container { height: 320px; min-height: 0; padding-left: 36px; padding-right: 36px; }
}

/* 首页新版信息层级：搜索 → 分类 → 猜你喜欢 → 推荐商家 */
.wrapper { min-height: 100vh; background: #f5f8fb; color: #314f64; }
.wrapper header { height: 56px; padding: 0 16px; background: #168bd1; }
.wrapper header .icon-location-box { width: 20px; height: 20px; margin-right: 7px; }
.wrapper header .icon-location-box i { font-size: 18px; }
.wrapper header .location-text { min-width: 0; max-width: 62%; font-size: 15px; font-weight: 500; }
.wrapper header .login-register { margin-left: auto; gap: 7px; }
.wrapper header .login-register button { padding: 6px 10px; border-radius: 14px; font-size: 12px; }
.wrapper .search { height: 68px; }
.wrapper .search .search-fixed-top { height: 68px; padding: 10px 14px; box-sizing: border-box; background: #168bd1; }
.wrapper .search .search-fixed-top .search-box { width: 100%; height: 46px; padding: 0 6px 0 15px; box-sizing: border-box; border: 1px solid #cce9f7; border-radius: 24px; background: #fff; color: #91a7b5; font-size: 14px; }
.wrapper .search .search-fixed-top .search-box input { margin: 0 8px; font-size: 14px; }
.wrapper .search .search-fixed-top .search-box .search-btn { min-width: 58px; padding: 9px 13px; border-radius: 19px; background: #168bd1; font-size: 13px; }
.wrapper .foodtype { height: auto; padding: 12px 10px 9px; display: grid; grid-template-columns: repeat(5, 1fr); gap: 9px 4px; align-content: initial; box-sizing: border-box; border-bottom: 1px solid #e5edf2; }
.wrapper .foodtype li { width: auto; height: 66px; gap: 4px; }
.wrapper .foodtype li img { width: 38px; height: 34px; object-fit: contain; }
.wrapper .foodtype li p { color: #5d7484; font-size: 12px; }
.guess-section { padding: 13px 14px 12px; background: #fff; border-bottom: 1px solid #e5edf2; }
.guess-section .section-heading { display: flex; align-items: center; justify-content: space-between; margin: 0 1px 10px; }
.guess-section .section-heading > div { display: flex; align-items: baseline; gap: 8px; }
.guess-section .section-heading h2 { color: #31556d; font-size: 17px; }
.guess-section .section-heading span { color: #9aadb9; font-size: 11px; }
.guess-section .section-heading button { border: 0; background: transparent; color: #8aa0af; font-size: 12px; cursor: pointer; }
.guess-scroll { display: flex; gap: 10px; overflow-x: auto; scrollbar-width: none; }
.guess-scroll::-webkit-scrollbar { display: none; }
.guess-card { flex: 0 0 145px; min-width: 0; padding: 8px; border: 1px solid #e1edf4; border-radius: 8px; background: #fff; text-align: left; cursor: pointer; }
.guess-card img { width: 100%; height: 72px; object-fit: cover; border-radius: 6px; background: #f1f6f8; }
.guess-card strong { display: block; margin-top: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #3c5f74; font-size: 13px; }
.guess-card span { display: block; margin-top: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #94a7b3; font-size: 10px; }
.guess-card b { color: #e58a4e; font-weight: 500; }
.wrapper .recommend { display: flex; align-items: center; justify-content: flex-start; gap: 10px; width: 100%; height: auto; min-height: 52px; box-sizing: border-box; margin: 0; padding: 17px 14px 9px; background: #f5f8fb; }
.wrapper .recommend p { color: #31556d; font-size: 18px; font-weight: 600; }
.wrapper .recommend .recommend-line { display: none; }
.wrapper .recommendtype { position: sticky; top: 0; z-index: 8; width: 100%; height: 44px; margin-bottom: 0; padding: 0 14px; box-sizing: border-box; justify-content: flex-start; gap: 23px; background: #f5f8fb; border-bottom: 1px solid #e2ebf1; }
.wrapper .recommendtype li { width: auto; height: 44px; padding: 13px 0 10px; color: #6e8798; font-size: 13px; }
.wrapper .recommendtype li.active { color: #168bd1; font-weight: 600; border-bottom: 2px solid #168bd1; }
.wrapper .business-list { width: 100%; margin: 0; padding: 0 12px 80px; box-sizing: border-box; }
.wrapper .business-list li { width: 100%; margin: 0 0 10px; padding: 12px; box-sizing: border-box; border: 1px solid #e1edf4; border-radius: 9px; background: #fff; box-shadow: 0 2px 7px rgba(39,86,114,.05); cursor: pointer; }
.wrapper .business-list li:hover { transform: none; box-shadow: 0 2px 7px rgba(39,86,114,.05); }
.wrapper .business-list li .business-info { display: flex; align-items: flex-start; gap: 11px; }
.wrapper .business-list li .business-info img { width: 98px; height: 98px; flex: 0 0 98px; margin: 0; border-radius: 7px; object-fit: cover; background: #f0f5f8; }
.wrapper .business-list li .business-info .business-info-detail { min-width: 0; flex: 1; padding-top: 1px; }
.wrapper .business-list li .business-info .business-info-detail h3 { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #2f526a; font-size: 16px; line-height: 1.35; }
.wrapper .business-list li .business-info .business-info-rating { display: flex; align-items: baseline; gap: 8px; margin-top: 6px; }
.wrapper .business-list li .business-info .business-info-rating .rating-score { color: #e07b45; font-size: 15px; font-weight: 600; }
.wrapper .business-list li .business-info .business-info-rating .monthly-sales,
.wrapper .business-list li .business-info .business-info-rating .average-price { color: #8399a8; font-size: 11px; }
.wrapper .business-list li .business-info .business-info-delivery { display: flex; gap: 10px; margin-top: 7px; color: #708797; font-size: 11px; }
.wrapper .business-list li .business-info .business-info-delivery .start-price,
.wrapper .business-list li .business-info .business-info-delivery .delivery-fee { color: #708797; font-size: 11px; line-height: 1.4; }
.wrapper .business-list li .business-info .business-info-delivery .free-delivery { color: #168bd1; }
.business-tags { display: flex; flex-wrap: wrap; gap: 5px; margin-top: 8px; }
.business-tag { display: inline-block; max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; padding: 3px 6px; border: 1px solid #d9e7ee; border-radius: 3px; font-size: 10px; line-height: 1.1; }
.business-tag.tag-blue { border-color: #b9def1; background: #f1faff; color: #168bd1; }
.business-tag.tag-orange { border-color: #f1d0b7; background: #fff8f2; color: #d97b43; }
.business-tag.tag-gold { border-color: #f0dfb0; background: #fffbef; color: #b48731; }
.business-tag.tag-green { border-color: #c5e5d2; background: #f2fbf5; color: #3d9b69; }
.business-tag.tag-neutral { border-color: #dce7ed; background: #f8fbfc; color: #7591a0; }
.closed-shop-tag{margin-left:5px;padding:2px 6px;border-radius:7px;background:#edf1f4;color:#80909c;font-size:10px;font-weight:500;vertical-align:2px}
.wrapper .empty-business-list { padding: 50px 16px; }
.load-more { display: block; width: calc(100% - 28px); margin: 2px auto 82px; padding: 11px 0; border: 1px solid #c8e4f4; border-radius: 6px; background: #fff; color: #168bd1; font-size: 13px; cursor: pointer; }
.load-more:active { background: #f1faff; }
@media (min-width: 700px) {
    .wrapper { max-width: 600px; }
    .wrapper .search .search-fixed-top { max-width: 600px; margin: 0 auto; }
    .wrapper .business-list { padding-left: 0; padding-right: 0; }
}
</style>
