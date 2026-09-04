const configuredApiBaseUrl = process.env.VUE_APP_API_BASE_URL;
const isLocalBrowser = ['localhost', '127.0.0.1'].includes(window.location.hostname);

export const apiBaseUrl = configuredApiBaseUrl
  ? configuredApiBaseUrl.replace(/\/$/, '')
  : (isLocalBrowser ? 'http://localhost:18080' : window.location.origin);

export const getWebSocketUrl = (path) => {
  const url = new URL(path, `${apiBaseUrl}/`);
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
  const token = localStorage.getItem('token') || sessionStorage.getItem('token');
  if (token) url.searchParams.set('access_token', token);
  return url.toString();
};
