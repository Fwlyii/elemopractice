import { createRouter, createWebHistory } from 'vue-router'
import Index from '../views/Index.vue'

// 首页以外按路由懒加载，顾客不需为商家/骑手/管理端下载代码。
const BusinessList = () => import('../views/BusinessList.vue')
const BusinessInfo = () => import('../views/BusinessInfo.vue')
const Login = () => import('../views/Login.vue')
const UserAddress = () => import('../views/UserAddress.vue')
const Payment = () => import('../views/Payment.vue')
const OrderList = () => import('../views/OrderList.vue')
const ListDetail = () => import('../views/ListDetail.vue')
const AddUserAddress = () => import('../views/AddUserAddress.vue')
const EditUserAddress = () => import('../views/EditUserAddress.vue')
const Register = () => import('../views/Register.vue')
const SuccessfulPayment = () => import('../views/SuccessfulPayment.vue')
const MyInformation = () => import('../views/MyInformation.vue')
const Favorites = () => import('../views/Favorites.vue')
const Notifications = () => import('../views/Notifications.vue')
const Assets = () => import('../views/Assets.vue')
const Preferences = () => import('../views/Preferences.vue')
const Search = () => import('../views/Search.vue')
const Cart = () => import('../views/Cart.vue')
const AiChat = () => import('../views/AiChat.vue')
const RiderApply = () => import('../views/RiderApply.vue')
const RiderDashboard = () => import('../views/RiderDashboard.vue')
const MerchantProfile = () => import('../views/MerchantProfile.vue')
const MerchantBusiness = () => import('../views/MerchantBusiness.vue')
const MerchantBusinessInfo = () => import('../views/MerchantBusinessInfo.vue')
const MerchantOrders = () => import('../views/MerchantOrders.vue')
const MerchantReviews = () => import('../views/MerchantReviews.vue')
const MerchantApply = () => import('../views/MerchantApply.vue')
const AdminHome = () => import('../views/AdminHome.vue')
const AdminUser = () => import('../views/AdminUser.vue')
const AdminBusiness = () => import('../views/AdminBusiness.vue')
const AdminShop = () => import('../views/AdminShop.vue')
const AdminRiders = () => import('../views/AdminRiders.vue')

const routes = [
  {
    path: '/',
    redirect: '/index'
  },
  {
    path: '/index',
    name: 'Index',
    component: Index,
    meta: { public: true }
  },
  {
    path: '/businessList',
    name: 'BusinessList',
    component: BusinessList,
    meta: { public: true }
  },
  {
    path: '/businessInfo',
    name: 'BusinessInfo',
    component: BusinessInfo,
    meta: { public: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { public: true }
  },
  {
    path: '/userAddress',
    name: 'UserAddress',
    component: UserAddress,
    meta: { role: 'user' }
  },
  {
    path: '/payment',
    name: 'Payment',
    component: Payment,
    meta: { role: 'user' }
  },
  {
    path: '/orderList',
    name: 'OrderList',
    component: OrderList,
    meta: { role: 'user' }
  },
  {
    path: '/listDetail',
    name: 'ListDetail',
    component: ListDetail,
    meta: { role: 'user' }
  },
  {
    path: '/addUserAddress',
    name: 'AddUserAddress',
    component: AddUserAddress,
    meta: { role: 'user' }
  },
  {
    path: '/editUserAddress',
    name: 'EditUserAddress',
    component: EditUserAddress,
    meta: { role: 'user' }
  },
  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: { public: true }
  },
  {
    path: '/successfulPayment',
    name: 'SuccessfulPayment',
    component: SuccessfulPayment,
    meta: { role: 'user' }
  },
  {
    path: '/myInformation',
    name: 'MyInformation',
    component: MyInformation,
    meta: { role: 'user' }
  },
  {
    path: '/favorites',
    name: 'Favorites',
    component: Favorites,
    meta: { role: 'user' }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: Notifications
  },
  {
    path: '/assets',
    name: 'Assets',
    component: Assets,
    meta: { role: 'user' }
  },
  {
    path: '/preferences',
    name: 'Preferences',
    component: Preferences,
    meta: { role: 'user' }
  },
  {
    path: '/search',
    name: 'Search',
    component: Search,
    meta: { public: true }
  },
  {
    path: '/cart',
    name: 'Cart',
    component: Cart,
    meta: { role: 'user' }
  },
  {
    path: '/merchant/apply',
    name: 'MerchantApply',
    component: MerchantApply,
    meta: { title: '申请成为商家', role: 'merchant', allowApplicant: true }
  },
  {
    path: '/merchant/profile',
    name: 'MerchantProfile',
    component: MerchantProfile,
    meta: { title: '商家信息', role: 'merchant' }
  },
  {
    path: '/merchant/business',
    name: 'MerchantBusiness',
    component: MerchantBusiness,
    meta: { role: 'merchant' }
  },
  {
    path: '/merchant/businessInfo',
    name: 'MerchantBusinessInfo',
    component: MerchantBusinessInfo,
    meta: { role: 'merchant' }
  },
  {
    path: '/merchant/orders',
    name: 'MerchantOrders',
    component: MerchantOrders,
    meta: { role: 'merchant' }
  },
  {
    path: '/merchant/reviews',
    name: 'MerchantReviews',
    component: MerchantReviews,
    meta: { role: 'merchant' }
  },
  {
    path: '/ai-chat',
    name: 'AiChat',
    component: AiChat,
    meta: { title: 'AI智能客服', public: true }
  },
  {
    path: '/rider/apply',
    name: 'RiderApply',
    component: RiderApply,
    meta: { title: '申请成为骑手', role: 'rider', allowApplicant: true }
  },
  {
    path: '/rider/dashboard',
    name: 'RiderDashboard',
    component: RiderDashboard,
    meta: { title: '骑手工作台', role: 'rider' }
  },
  {
    path:'/admin/home',
    name:'AdminHome',
    component:AdminHome,
    meta: { role: 'admin' }
  },
  {
    path:'/admin/user',
    name:'AdminUser',
    component:AdminUser,
    meta: { role: 'admin' }
  },
  {
    path:'/admin/business',
    name:'AdminBusiness',
    component:AdminBusiness,
    meta: { role: 'admin' }
  },
  {
    path:'/admin/shop',
    name:'AdminShop',
    component:AdminShop,
    meta: { role: 'admin' }
  },
  {
    path:'/admin/riders',
    name:'AdminRiders',
    component:AdminRiders,
    meta: { role: 'admin' }
  },
  { path: '/:pathMatch(.*)*', redirect: '/index' }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

export default router
