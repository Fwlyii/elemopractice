import { createRouter, createWebHistory } from 'vue-router'
import Index from '../views/Index.vue'
import BusinessList from '../views/BusinessList.vue'
import BusinessInfo from '../views/BusinessInfo.vue'
import Login from '../views/Login.vue'
import UserAddress from '../views/UserAddress.vue'
import Payment from '../views/Payment.vue'
import OrderList from '../views/OrderList.vue'
import ListDetail from '../views/ListDetail.vue'
import AddUserAddress from '../views/AddUserAddress.vue'
import EditUserAddress from '../views/EditUserAddress.vue'
import Register from '../views/Register.vue'
import SuccessfulPayment from '../views/SuccessfulPayment.vue'
import MyInformation from '@/views/MyInformation.vue'
import Favorites from '@/views/Favorites.vue'
import Notifications from '@/views/Notifications.vue'
import Assets from '@/views/Assets.vue'
import Preferences from '@/views/Preferences.vue'
import AdminUser from '@/views/AdminUser.vue'
import Search from '@/views/Search.vue'
import SearchTest from '@/views/SearchTest.vue'
import Cart from '@/views/Cart.vue'
import BusinessInformation from '@/views/BusinessInformation.vue'
import BusinessView from '@/views/BusinessView.vue'
import SubmitItems from '@/views/SubmitItems.vue'
import AiChat from '@/views/AiChat.vue'
import RiderApply from '@/views/RiderApply.vue'
import RiderDashboard from '@/views/RiderDashboard.vue'
import AdminRiders from '@/views/AdminRiders.vue'

//商家端路由配置
import MerchantProfile from '../views/MerchantProfile.vue';
import MerchantBusiness from '../views/MerchantBusiness.vue'
import MerchantBusinessInfo from '@/views/MerchantBusinessInfo.vue' 
import MerchantOrders from '../views/MerchantOrders.vue'
import MerchantReviews from '../views/MerchantReviews.vue'

//管理端路由配置
import AdminHome from '../views/AdminHome.vue';
import AdminBusiness from '@/views/AdminBusiness.vue'
import AdminShop from '@/views/AdminShop.vue'
//import { pa } from 'element-plus/es/locale'

// 定义路由
const routes = [
  {
    path: '/',
    name: 'Home',
    component: Index
  },
  {
    path: '/Index',
    name: 'Index',
    component: Index
  },
  {
    path: '/businessList',
    name: 'BusinessList',
    component: BusinessList
  },
  {
    path: '/businessInfo',
    name: 'BusinessInfo',
    component: BusinessInfo
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/orders',
    redirect: '/orderList'
  },
  {
    path: '/userAddress',
    name: 'UserAddress',
    component: UserAddress
  },
  {
    path: '/payment',
    name: 'Payment',
    component: Payment
  },
  {
    path: '/orderList',
    name: 'OrderList',
    component: OrderList
  },
  {
    path: '/listDetail',
    name: 'ListDetail',
    component: ListDetail
  },
  {
    path: '/addUserAddress',
    name: 'AddUserAddress',
    component: AddUserAddress
  },
  {
    path: '/editUserAddress',
    name: 'EditUserAddress',
    component: EditUserAddress
  },
  {
    path: '/register',
    name: 'Register',
    component: Register
  },
  {
    path: '/successfulPayment',
    name: 'SuccessfulPayment',
    component: SuccessfulPayment
  },
  {
    path: '/myInformation',
    name: 'MyInformation',
    component: MyInformation
  },
  {
    path: '/favorites',
    name: 'Favorites',
    component: Favorites,
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: Notifications,
  },
  {
    path: '/assets',
    name: 'Assets',
    component: Assets,
  },
  {
    path: '/preferences',
    name: 'Preferences',
    component: Preferences,
  },
 //
  {
    path: '/search',
    name: 'Search',
    component: Search
  },
  {
    path: '/search-test',
    name: 'SearchTest',
    component: SearchTest
  },
  {
    path: '/cart',
    name: 'Cart',
    component: Cart
  },
  // 兼容上一版演示链接，统一跳转到新的商家工作台路径。
  { path:'/merchantOrders', redirect:'/merchant/orders' },
  { path:'/merchantProfile', redirect:'/merchant/profile' },
  { path:'/merchantBusiness', redirect:'/merchant/business' },
  { path:'/merchantBusinessInfo', redirect:'/merchant/businessInfo' },
  {
    path:'/businessInformation',
    name:'BusinessInformation',
    component:BusinessInformation
  },
  {
    path:'/businessView',
    name:'BusinessView',
    component:BusinessView
  },
  // {
  //   path:'/businessOrderManage',
  //   name:'BusinessOrderManage',
  //   component:BusinessOrderManage
  // },
  {
    path: '/merchant/profile',
    name: 'MerchantProfile',
    component: MerchantProfile,
    meta: { title: '商家信息' }
  },
  {
    path: '/merchant/business',
    name: 'MerchantBusiness',
    component: MerchantBusiness
  },
  {
    path: '/merchant/businessInfo',
    name: 'MerchantBusinessInfo',
    component: MerchantBusinessInfo
  },
  {
    path: '/merchant/orders',
    name: 'MerchantOrders',
    component: MerchantOrders
  },
  { path: '/merchant/reviews', name: 'MerchantReviews', component: MerchantReviews },
  {
    path:'/submitItems',
    name:'SubmitItems',
    component:SubmitItems
  },
  {
    path: '/ai-chat',
    name: 'AiChat',
    component: AiChat,
    meta: { title: 'AI智能客服' }
  },
  {
    path: '/rider/apply',
    name: 'RiderApply',
    component: RiderApply,
    meta: { title: '申请成为骑手' }
  },
  {
    path: '/rider/dashboard',
    name: 'RiderDashboard',
    component: RiderDashboard,
    meta: { title: '骑手工作台' }
  },
 //
//管理端
  {
    path:'/admin/home',
    name:'AdminHome',
    component:AdminHome
  },
  {
    path:'/admin/user',
    name:'AdminUser',
    component:AdminUser
  },
  {
    path:'/admin/business',
    name:'AdminBusiness',
    component:AdminBusiness
  },
  {
    path:'/admin/shop',
    name:'AdminShop',
    component:AdminShop
  },
  {
    path:'/admin/riders',
    name:'AdminRiders',
    component:AdminRiders
  }
  //

]
  
  //
 //



// 解决重复路由报异常问题
const originalPush = createRouter.prototype.push;
createRouter.prototype.push = function push(location) {
  return originalPush.call(this, location).catch(err => err)
}

// 创建路由实例
const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

export default router
