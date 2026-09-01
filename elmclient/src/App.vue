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
  </div>
</template>

<script>
import BackButton from './components/BackButton.vue';
import Footer from './components/Footer.vue';
import BusinessFooter from './components/BusinessFooter.vue';
import AdminFooter from './components/AdminFooter.vue';
import { computed } from 'vue';
import { useRoute } from 'vue-router';

export default {
  components: {
    BackButton,
    Footer,
    BusinessFooter,
    AdminFooter,
  },
  setup() {
    const route = useRoute();

    const showBackButton = computed(() => {
      if (route.path.startsWith('/merchant') || route.path.startsWith('/admin')) {
        return false;
      }
         });


    const showFooter = computed(() => {
      if (route.path.startsWith('/merchant') || route.path.startsWith('/admin')) {
        return false;
      }
      return !['BusinessInfo', 'Payment', 'SuccessfulPayment', 'Orders', 'Cart','Favorites','Notifications','UserAddress','ListDetail','Register','Login','EditUserAddress'].includes(route.name);
    });

    const showBusinessFooter = computed(() => {
     if(route.path=== '/merchant/businessinfo'){
        return false;
      }
      return route.path.startsWith('/merchant');
      
    });

    const showAdminFooter = computed(() => {
      return route.path.startsWith('/admin');
    });

    return { showFooter, showBusinessFooter, showAdminFooter, showBackButton};
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