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
import { getRoleDefinition, roleCanEnter } from './utils/roles';
import { getStoredUser } from './utils/auth';
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
  const user = getStoredUser();

  // 路由自己声明所需角色；新增工作台页只需配置 meta，不再修改守卫分支。
  const roleKey = typeof to.meta.role === 'string' ? to.meta.role : null;
  if (roleKey) {
    const role = getRoleDefinition(roleKey);
    if (!user) return next(`/login?role=${roleKey}`);
    if (!roleCanEnter(user, roleKey)) return next(role.applyTarget || '/myInformation');
  }
  if (to.path === '/myInformation' && to.query.role === 'rider') {
    if (!user) return next('/login?role=rider');
    if (!roleCanEnter(user, 'rider')) return next('/rider/apply');
  }

  
  // 浏览商家和登录注册可匿名访问，其余功能都需要登录。
  if (!to.meta.public && !user) {
    return next({ path: '/login', query: { redirect: to.fullPath } });
  }
  next();
});

// 使用 Vue Router
app.use(router);

// 挂载 Vue 应用
app.mount('#app');
