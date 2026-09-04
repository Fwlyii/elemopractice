<template>
  <main class="login-page">
    <section class="login-shell">
      <aside class="login-intro">
        <div class="intro-logo"><i class="fas fa-utensils"></i></div>
        <p class="eyebrow">饿了么</p>
        <h1>选择登录端</h1>
      </aside>

      <form class="login-card" @submit.prevent="login">
        <div class="card-heading">
          <p class="card-kicker">账号登录</p>
          <h2>{{ activeRole.title }}</h2>
          <p>{{ activeRole.subtitle }}</p>
        </div>

        <div class="role-grid" role="tablist" aria-label="登录身份">
          <button
            v-for="item in roleOptions"
            :key="item.key"
            type="button"
            class="role-item"
            :class="{ selected: selectedRole === item.key }"
            @click="selectRole(item.key)"
          >
            <i :class="item.icon"></i>
            <span>{{ item.label }}</span>
          </button>
        </div>

        <label class="field">
          <span>用户名</span>
          <div class="input-wrap"><i class="fas fa-user"></i><input v-model.trim="userName" type="text" autocomplete="username" placeholder="请输入用户名" /></div>
        </label>
        <label class="field">
          <span>密码</span>
          <div class="input-wrap"><i class="fas fa-lock"></i><input v-model="password" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" placeholder="请输入密码" /><button class="password-toggle" type="button" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword"><i :class="showPassword ? 'fas fa-eye-slash' : 'fas fa-eye'"></i></button></div>
        </label>

        <div class="form-options">
          <label><input v-model="rememberMe" type="checkbox" /> 记住我</label>
          <span v-if="savedUserName">上次登录：{{ savedUserName }}</span>
        </div>
        <button class="login-button" type="submit" :disabled="submitting">
          <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
          {{ submitting ? '正在登录…' : activeRole.button }}
        </button>
        <p v-if="selectedRole === 'user'" class="register-hint">还没有账号？ <router-link to="/register">立即注册</router-link></p>
      </form>
    </section>
  </main>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '../utils/request';
import { toast } from '../utils/toast';
import { ROLE_DEFINITIONS, roleCanEnter } from '../utils/roles';
import { clearAuth, saveAuth, updateStoredUser } from '../utils/auth';

const router = useRouter();
const route = useRoute();
const roleMap = ROLE_DEFINITIONS;
const roleOptions = Object.values(roleMap);
const queryRole = typeof route.query.role === 'string' && roleMap[route.query.role] ? route.query.role : 'user';
const selectedRole = ref(queryRole);
const userName = ref(localStorage.getItem('savedUserName') || '');
const password = ref('');
const showPassword = ref(false);
const rememberMe = ref(false);
const submitting = ref(false);
const savedUserName = computed(() => localStorage.getItem('savedUserName') || '');
const activeRole = computed(() => roleMap[selectedRole.value]);

watch(() => route.query.role, value => {
  if (typeof value === 'string' && roleMap[value]) selectedRole.value = value;
});

const selectRole = (key) => {
  selectedRole.value = key;
  router.replace({ query: { ...route.query, role: key } });
};

const login = async () => {
  if (!userName.value) return toast.error('请输入用户名');
  if (!password.value) return toast.error('请输入密码');
  submitting.value = true;
  try {
    const auth = await request.post('/api/auth', { username: userName.value, password: password.value, rememberMe: rememberMe.value });
    if (!auth?.id_token) return toast.error(auth?.message || '登录失败');
    // 登录身份切换时先清掉上一个账号的认证信息，避免路由守卫读到旧的 localStorage。
    // 这对“记住我”后再切换用户/商家/骑手尤其重要。
    saveAuth(auth.id_token, null, rememberMe.value);
    const userRes = await request.get('/api/user');
    updateStoredUser(userRes);
    if (rememberMe.value) localStorage.setItem('savedUserName', userName.value);
    else localStorage.removeItem('savedUserName');

    const roleAllowed = roleCanEnter(userRes, selectedRole.value);
    if (!roleAllowed) {
      const roleName = activeRole.value.label;
      toast.warning(`当前账号没有${roleName}权限，请切换身份或先完成申请`);
      if (activeRole.value.applyTarget) router.push(activeRole.value.applyTarget);
      return;
    }
	    toast.success(`已进入${activeRole.value.label}端`);
	    const redirect = typeof route.query.redirect === 'string'
	      && route.query.redirect.startsWith('/')
	      && !route.query.redirect.startsWith('//')
	      ? route.query.redirect
	      : null;
	    router.push(selectedRole.value === 'user' && redirect ? redirect : activeRole.value.target);
  } catch (error) {
    clearAuth();
    toast.error(error.response?.data?.message || '用户名或密码错误');
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
*{box-sizing:border-box}.login-page{min-height:100vh;background:#f5f9fd;display:flex;align-items:center;justify-content:center;padding:32px 20px;color:#24405c}.back-btn-container{position:fixed;top:18px;left:20px;z-index:10}.login-shell{width:min(100%,980px);min-height:590px;background:#fff;border:1px solid #e1edf8;border-radius:12px;display:grid;grid-template-columns:42% 58%;overflow:hidden;box-shadow:0 12px 35px rgba(45,100,155,.09)}.login-intro{padding:68px 52px;background:#eaf5ff;border-right:1px solid #dcecf9;display:flex;flex-direction:column;justify-content:center}.intro-logo{width:52px;height:52px;border-radius:10px;background:#0097ff;color:#fff;display:grid;place-items:center;font-size:22px;margin-bottom:28px}.eyebrow{font-size:10px;color:#4f8ac0;letter-spacing:1.4px;font-weight:700}.login-intro h1{font-size:36px;line-height:1.25;margin:18px 0;color:#173b60;font-weight:700}.login-intro h1 span{color:#0097ff}.intro-copy{color:#69839d;font-size:14px;line-height:1.9;max-width:280px}.intro-line{display:flex;align-items:center;gap:10px;margin-top:46px;color:#7592ad;font-size:11px}.intro-line span{width:30px;border-top:1px solid #95c8ed}.login-card{padding:58px 70px;display:flex;flex-direction:column;justify-content:center}.card-kicker{margin:0;color:#5f87aa;font-size:12px}.card-heading h2{font-size:28px;margin:8px 0;color:#183b5e}.card-heading>p:last-child{font-size:13px;color:#8096ab;margin-bottom:25px}.role-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;margin-bottom:24px}.role-item{border:1px solid #dfebf5;background:#fff;border-radius:7px;height:64px;color:#7b92a7;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:6px;cursor:pointer;font-size:11px}.role-item i{font-size:17px}.role-item:hover,.role-item.selected{border-color:#80c5f4;background:#edf8ff;color:#007fda}.field{display:block;margin-bottom:16px}.field>span{display:block;font-size:12px;color:#526f8b;font-weight:600;margin-bottom:7px}.input-wrap{height:44px;display:flex;align-items:center;gap:9px;padding:0 13px;border:1px solid #d8e5f0;border-radius:6px;background:#fff}.input-wrap:focus-within{border-color:#66b9ee;box-shadow:0 0 0 3px #e8f5ff}.input-wrap i{color:#98aec1;font-size:13px}.input-wrap input{border:0;outline:0;flex:1;font-size:14px;color:#29455f}.form-options{display:flex;justify-content:space-between;align-items:center;color:#8297aa;font-size:11px;margin:1px 0 21px}.form-options label{display:flex;align-items:center;gap:5px}.form-options input{accent-color:#0097ff}.login-button{height:45px;border:0;border-radius:6px;background:#0097ff;color:#fff;font-weight:600;font-size:15px;cursor:pointer;box-shadow:0 5px 12px rgba(0,151,255,.2)}.login-button:hover{background:#007fd8}.login-button:disabled{opacity:.7;cursor:wait}.register-hint{text-align:center;color:#8297aa;font-size:12px;margin:18px 0 0}.register-hint a{color:#008be7}.security-note{text-align:center;color:#a0afbd;font-size:10px;margin:20px 0 0}.security-note i{color:#7db9e5;margin-right:4px}@media(max-width:760px){.login-page{padding:18px 14px}.login-shell{display:block;min-height:0}.login-intro{padding:28px 26px;min-height:205px}.intro-logo{width:42px;height:42px;font-size:18px;margin-bottom:14px}.login-intro h1{font-size:27px;margin:10px 0}.intro-copy{font-size:12px;line-height:1.6}.intro-line{margin-top:18px}.login-card{padding:30px 24px 34px}.role-item{height:58px}.card-heading h2{font-size:24px}}
.input-wrap input{min-width:0}.password-toggle{border:0;background:transparent;color:#7592aa;padding:6px;cursor:pointer}.password-toggle i{color:inherit}
</style>
