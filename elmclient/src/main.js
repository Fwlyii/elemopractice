import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import { toast } from './utils/toast';
//import { getToken,removeToken} from './utils/auth';
import BackButton from '@/components/BackButton.vue';
import 'font-awesome/css/font-awesome.min.css';
import './assets/styles/global.css';
import qs from 'qs';
import request from './utils/request';
import { hasAuthority as userHasAuthority } from './utils/roles';
import {
  getCurDate,
  setSessionStorage,
  getSessionStorage,
  removeSessionStorage,
  setLocalStorage,  
  getLocalStorage,
  removeLocalStorage
} from './common.js';


// 创建 Vue 应用实例
const app = createApp(App);
app.component('BackButton', BackButton);
// 将 axios 挂载到 Vue 实例上
app.config.globalProperties.$axios = request;
app.config.globalProperties.$qs = qs;
//app.config.globalProperties.$api = api

app.config.globalProperties.$getCurDate = getCurDate;
app.config.globalProperties.$setSessionStorage = setSessionStorage;
app.config.globalProperties.$getSessionStorage = getSessionStorage;
app.config.globalProperties.$removeSessionStorage = removeSessionStorage;
app.config.globalProperties.$setLocalStorage = setLocalStorage;
app.config.globalProperties.$getLocalStorage = getLocalStorage;
app.config.globalProperties.$removeLocalStorage = removeLocalStorage;

// 注册全局 toast 服务
app.config.globalProperties.$toast = toast;

// 路由守卫
router.beforeEach((to, from, next) => {
  const parseStoredUser = (storage, key) => {
    try {
      const value = storage.getItem(key);
      return value ? JSON.parse(value) : null;
    } catch (_) {
      storage.removeItem(key);
      return null;
    }
  };
  const businessUser = parseStoredUser(sessionStorage, 'businessUser');
  const userFromLocal = parseStoredUser(localStorage, 'userInfo');
  const userFromSession = parseStoredUser(sessionStorage, 'userInfo');
  const user = userFromLocal || userFromSession;

  const hasRoleAuthority = (authority) => {
    if (authority === 'BUSINESS' && businessUser?.isBusiness) return true;
    return userHasAuthority(user, authority);
  };

  // 四个工作台必须与账号身份匹配。角色入口集中在这里，页面不再各写一套判断。
  const roleArea = to.path.startsWith('/merchant/') ? { authority: 'BUSINESS', loginRole: 'merchant', denied: '/myInformation' }
    : to.path.startsWith('/admin/') ? { authority: 'ADMIN', loginRole: 'admin', denied: '/myInformation' }
      : to.path.startsWith('/rider/dashboard') ? { authority: 'RIDER', loginRole: 'rider', denied: '/rider/apply' }
        : null;
  if (roleArea) {
    if (!user && !businessUser) return next(`/login?role=${roleArea.loginRole}`);
    if (!hasRoleAuthority(roleArea.authority)) return next(roleArea.denied);
  }
  if (to.path === '/myInformation' && to.query.role === 'rider') {
    if (!user) return next('/login?role=rider');
    if (!hasRoleAuthority('RIDER')) return next('/rider/apply');
  }

  
  // 浏览商家和登录注册可匿名访问，其余功能都需要登录。
  const publicRouteNames = new Set(['Home', 'Index', 'BusinessList', 'BusinessInfo', 'Search', 'Login', 'Register']);
  if (!publicRouteNames.has(to.name) && !user && !businessUser) {
    return next({ path: '/login', query: { redirect: to.fullPath } });
  }
  next();
});

// 使用 Vue Router
app.use(router);

// 挂载 Vue 应用
app.mount('#app');
