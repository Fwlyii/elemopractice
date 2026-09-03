<template>
  <div class="app-container">
    <BackButton v-if="showBackButton" />
    <div class="content">
      <keep-alive include="Discover">
        <router-view />
      </keep-alive>
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
      const token = localStorage.getItem('token') || sessionStorage.getItem('token');
      if (!token) return;
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
      // 这些页面已有自己的返回按钮，或本身就是一级页面；不要再叠一层全局按钮。
      return !['Home', 'Index', 'MyInformation', 'SuccessfulPayment', 'BusinessInfo', 'UserAddress', 'Assets', 'AiChat'].includes(route.name);
         });


    const showFooter = computed(() => {
      if (route.path.startsWith('/merchant') || route.path.startsWith('/admin') || isRiderArea.value) {
        return false;
      }
      return !['BusinessInfo', 'Payment', 'SuccessfulPayment', 'Orders', 'Cart','Favorites','Notifications','UserAddress','AddUserAddress','ListDetail','Register','Login','EditUserAddress'].includes(route.name);
    });

    const showBusinessFooter = computed(() => {
     if(route.name === 'MerchantBusinessInfo'){
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

    const showAdminFooter = computed(() => {
      return route.path.startsWith('/admin');
    });

    return { showFooter, showBusinessFooter, showAdminFooter, showRiderFooter, showBackButton};
  },
};
</script>

<style>
/* 保持所有原有样式不变 */
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
