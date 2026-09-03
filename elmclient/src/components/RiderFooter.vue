<template>
  <nav class="role-footer" aria-label="骑手导航">
    <router-link
      to="/rider/dashboard?tab=available"
      class="nav-item"
      :class="{ active: isActive('available') }"
    >
      <i class="fas fa-list-alt"></i>
      <span>接单广场</span>
    </router-link>
    <router-link
      to="/rider/dashboard?tab=active"
      class="nav-item"
      :class="{ active: isActive('active') }"
    >
      <i class="fas fa-route"></i>
      <span>我的订单</span>
    </router-link>
    <router-link
      to="/myInformation?role=rider"
      class="nav-item"
      :class="{ active: route.path === '/myInformation' && route.query.role === 'rider' }"
    >
      <i class="fas fa-user"></i>
      <span>我的</span>
    </router-link>
  </nav>
</template>

<script setup>
import { useRoute } from 'vue-router';

const route = useRoute();
const isActive = (tab) => {
  if (route.path !== '/rider/dashboard') return false;
  const currentTab = route.query.tab || 'available';
  return tab === 'active' ? ['active', 'history'].includes(currentTab) : currentTab === tab;
};
</script>

<style scoped>
.role-footer {
  position: fixed;
  z-index: 1200;
  left: 0;
  right: 0;
  bottom: 0;
  height: 62px;
  display: flex;
  align-items: stretch;
  justify-content: center;
  background: #fff;
  border-top: 1px solid #e6eef7;
  box-shadow: 0 -3px 12px rgba(31, 75, 122, .06);
}

.nav-item {
  width: min(33.333%, 220px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #8091a5;
  text-decoration: none;
  font-size: 12px;
  transition: color .18s ease;
}

.nav-item i { font-size: 18px; }
.nav-item.active { color: #0097ff; font-weight: 600; }
.nav-item:hover { color: #0097ff; }
</style>
