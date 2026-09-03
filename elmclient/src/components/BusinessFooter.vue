<template>
  <nav class="role-footer" aria-label="商家导航">
    <router-link to="/merchant/business" class="nav-item" :class="{ active: route.path === '/merchant/business' }">
      <i class="fas fa-chart-line"></i>
      <span>工作台</span>
    </router-link>
    <router-link to="/merchant/orders" class="nav-item" :class="{ active: route.path === '/merchant/orders' }">
      <i class="fas fa-clipboard-list"></i>
      <span>订单</span>
    </router-link>
    <router-link to="/merchant/profile" class="nav-item" :class="{ active: route.path === '/merchant/profile' }">
      <i class="fas fa-user"></i>
      <span>我的</span>
    </router-link>
  </nav>
</template>

<script setup>
import { useRoute } from 'vue-router';

const route = useRoute();
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
