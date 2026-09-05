import axios from 'axios';
import { apiBaseUrl } from './endpoints';
import { clearAuth, getToken } from './auth';

// 1. 创建 Axios 实例
const request = axios.create({
  baseURL: apiBaseUrl,
  timeout: 15000, // 请求超时时间增加到15秒
  headers: {
    'Content-Type': 'application/json' // 默认请求格式
  }
});

// 2. 请求拦截器：给非登录/注册请求加 Authorization 头
request.interceptors.request.use(
  (config) => {
    const apiOrigin = new URL(apiBaseUrl, window.location.origin).origin;
    const targetUrl = new URL(config.url || '', apiBaseUrl.endsWith('/') ? apiBaseUrl : `${apiBaseUrl}/`);
    // 这个 Axios 实例只允许访问本项目后端，防止绝对 URL 或重定向配置把 JWT 带到第三方站点。
    if (targetUrl.origin !== apiOrigin) {
      return Promise.reject(new Error('拒绝向非平台域名发送认证请求'));
    }
    const excludePaths = ['/api/auth', '/api/register'];
    if (!excludePaths.includes(targetUrl.pathname)) {
      // 从 localStorage/sessionStorage 获取 token
      const token = getToken();
      if (token) {
        config.headers.Authorization = `Bearer ${token}`; // 拼接 Bearer 格式
        // 临时注释掉token头，避免CORS问题
        // config.headers.token = token; // 添加 token 头
      }
    }
    const isFormData = typeof FormData !== 'undefined' && config.data instanceof FormData;
    if (isFormData) {
      // multipart 的 boundary 必须由浏览器生成，不能沿用 Axios 实例的 JSON Content-Type。
      delete config.headers['Content-Type'];
    } else if (config.method === 'post' && typeof config.data === 'object') {
      // 其他 POST 请求处理
      config.data = JSON.stringify(config.data);
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 3. 响应拦截器：统一处理错误（如 token 过期）
request.interceptors.response.use(
  (response) => {
    return response.data; // 直接返回响应体，简化后续处理
  },
  (error) => {
    // 只有“原本携带了登录态”的 401 才表示会话过期。游客请求公开页面时即使某个
    // 可选接口返回 401，也不能被全局拦截器强行踢到登录页，否则退出登录后会形成跳转循环。
    const hadToken = Boolean(getToken());
    if (error.response?.status === 401 && hadToken && !error.config?.skipAuthRedirect) {
      clearAuth();
      const currentPath = `${window.location.pathname}${window.location.search}`;
      const redirect = currentPath.startsWith('/login') ? '' : `?redirect=${encodeURIComponent(currentPath)}`;
      window.location.assign(`/login${redirect}`);
    }
    return Promise.reject(error);
  }
);

export default request;
