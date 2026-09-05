<template>
  <div class="app-container">
    <BackButton v-if="showBackButton" />
    <div class="content">
      <router-view />
    </div>
    <Footer v-if="showFooter" />
    <BusinessFooter v-if="showBusinessFooter" />
    <AdminFooter v-if="showAdminFooter" />
    <RiderFooter v-if="showRiderFooter" />
  </div>
</template>

<script>
import BackButton from './components/BackButton.vue';
import Footer from './components/Footer.vue';
import BusinessFooter from './components/BusinessFooter.vue';
import AdminFooter from './components/AdminFooter.vue';
import RiderFooter from './components/RiderFooter.vue';
import { computed, onMounted, nextTick, watch } from 'vue';
import { useRoute } from 'vue-router';
import request from './utils/request';
import { applyTheme, getStoredTheme } from './utils/theme';
import { getAuthRole, getToken } from './utils/auth';

const ROUTES_WITHOUT_GLOBAL_BACK = new Set([
  'Index', 'MyInformation', 'SuccessfulPayment', 'BusinessInfo',
  'UserAddress', 'Assets', 'AiChat', 'Favorites', 'ListDetail'
]);

const ROUTES_WITHOUT_CUSTOMER_FOOTER = new Set([
  'BusinessInfo', 'Payment', 'SuccessfulPayment', 'Cart', 'Favorites',
  'Notifications', 'UserAddress', 'AddUserAddress', 'ListDetail',
  'Register', 'Login', 'EditUserAddress'
]);

export default {
  components: {
    BackButton,
    Footer,
    BusinessFooter,
    AdminFooter,
    RiderFooter,
  },
  setup() {
    const route = useRoute();

    // 路由切换时把应用自己的滚动容器归零，避免从长列表进入个人页时标题被“顶”到视口中间。
    watch(() => route.fullPath, () => {
      nextTick(() => {
        document.querySelector('.content')?.scrollTo({ top: 0, left: 0, behavior: 'auto' });
      });
    }, { immediate: true });

    // 先应用本地主题避免刷新闪白；登录用户再从服务端恢复跨设备偏好。
    onMounted(async () => {
      applyTheme(getStoredTheme());
      if (!getToken() || getAuthRole() !== 'user') return;
      try {
        const preference = await request.get('/api/v1/preferences/me');
        if (preference?.success && preference.data?.theme) applyTheme(preference.data.theme);
      } catch (_) {
        // 主题读取失败不应阻塞页面，保留本地主题继续使用。
      }
    });

    const showBackButton = computed(() => {
      if (route.path.startsWith('/merchant') || route.path.startsWith('/admin') || route.path.startsWith('/rider')) {
        return false;
      }
      return !ROUTES_WITHOUT_GLOBAL_BACK.has(route.name);
    });

    const showFooter = computed(() => {
      const isMerchantApplication = route.name === 'MerchantApply';
      if ((route.path.startsWith('/merchant') && !isMerchantApplication) || route.path.startsWith('/admin') || isRiderArea.value) {
        return false;
      }
      return !ROUTES_WITHOUT_CUSTOMER_FOOTER.has(route.name);
    });

    const showBusinessFooter = computed(() => {
      if (['MerchantBusinessInfo', 'MerchantApply'].includes(route.name)) {
        return false;
      }
      return route.path.startsWith('/merchant');
    });

    const isRiderArea = computed(() => {
      return route.path.startsWith('/rider') ||
        (route.query.role === 'rider' && ['/myInformation', '/notifications'].includes(route.path));
    });

    const isRiderContext = computed(() => {
      return route.path === '/rider/dashboard' ||
        (route.query.role === 'rider' && ['/myInformation', '/notifications'].includes(route.path));
    });

    const showRiderFooter = computed(() => isRiderContext.value);

    const showAdminFooter = computed(() => route.path.startsWith('/admin'));

    return { showFooter, showBusinessFooter, showAdminFooter, showRiderFooter, showBackButton };
  },
};
</script>

<style>
html,
body,
div,
span,
h1,
h2,
h3,
h4,
h5,
h6,
ul,
ol,
li,
p {
  margin: 0;
  padding: 0;
}

html,
body,
#app {
  width: 100%;
  height: 100%;
  font-family: "微软雅黑";
}

html,
body {
  margin: 0;
  padding: 0;
  height: 100%;
}

ul,
ol {
  list-style: none;
}

a {
  text-decoration: none;
}
.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.content {
  flex: 1;
  overflow-y: auto;
}
</style>
