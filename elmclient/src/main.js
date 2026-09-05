import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import { installAuthGuard } from './router/authGuard';
import 'font-awesome/css/font-awesome.min.css';
import './assets/styles/global.css';

installAuthGuard(router);
createApp(App).use(router).mount('#app');
