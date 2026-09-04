const TOKEN_KEY = 'token';
const USER_KEY = 'userInfo';

const parseJson = (storage, key) => {
  try {
    const value = storage.getItem(key);
    return value ? JSON.parse(value) : null;
  } catch (_) {
    storage.removeItem(key);
    return null;
  }
};

/** 登录态的唯一读写入口，避免页面各自猜 local/sessionStorage。 */
export const getToken = () => localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY);

export const getStoredUser = () => parseJson(localStorage, USER_KEY) || parseJson(sessionStorage, USER_KEY);

export const isAuthenticated = () => Boolean(getToken());

export const authStorage = (rememberMe = false) => rememberMe ? localStorage : sessionStorage;

export const saveAuth = (token, user, rememberMe = false) => {
  clearAuth();
  const storage = authStorage(rememberMe);
  storage.setItem(TOKEN_KEY, token);
  storage.setItem(USER_KEY, JSON.stringify(user));
};

export const updateStoredUser = (user) => {
  const storage = localStorage.getItem(TOKEN_KEY) ? localStorage
    : (sessionStorage.getItem(TOKEN_KEY) ? sessionStorage : null);
  if (storage) storage.setItem(USER_KEY, JSON.stringify(user));
};

export const clearAuth = () => {
  [localStorage, sessionStorage].forEach(storage => {
    storage.removeItem(TOKEN_KEY);
    storage.removeItem(USER_KEY);
  });
  sessionStorage.removeItem('businessUser');
};
