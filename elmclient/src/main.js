import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import { toast } from './utils/toast';
//import { getToken,removeToken} from './utils/auth';
import BackButton from '@/components/BackButton.vue';
import 'font-awesome/css/font-awesome.min.css';
import qs from 'qs';
import request from './utils/request';
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
  const businessUser = sessionStorage.getItem('businessUser') ? JSON.parse(sessionStorage.getItem('businessUser')) : null;
  const userFromLocal = localStorage.getItem('userInfo') ? JSON.parse(localStorage.getItem('userInfo')) : null;
  const userFromSession = sessionStorage.getItem('userInfo') ? JSON.parse(sessionStorage.getItem('userInfo')) : null;
  const user = userFromLocal || userFromSession;
  console.log(user);

  
  // 商家专属页面的路径
  const businessPaths = ['/businessView', '/businessInformation', '/submitItems'];
  
  //如果是访问商家专属页面
  if (businessPaths.includes(to.path)) {
    if (!businessUser || !businessUser.isBusiness) {
      // 如果没有商家登录，重定向到首页
      return next('/index');
    }
  }
  
  //普通用户页面的验证逻辑
  if (!(to.path === '/' || to.path === '/index' || to.path === '/businessList' || 
      to.path === '/businessInfo' || to.path === '/login' || to.path === '/register' || 
      to.path === '/lChoose' || to.path === '/rChoose' || to.path === '/businessLogin' || 
      to.path === '/businessRegister')) {
        console.log(user);
    if (user === null && !businessUser) {
      console.log('haha');
      return next('/login');
    }
  }
  console.log('lala');
  next();
});

// 使用 Vue Router
app.use(router);

// 挂载 Vue 应用
app.mount('#app');