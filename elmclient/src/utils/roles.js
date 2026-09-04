/**
 * 角色权限的唯一判断入口。
 * 页面只关心“当前用户是否拥有某权限”，避免各页面复制 authorities.some(...)
 * 逻辑，后续调整角色名称时只需修改这里和登录页的角色配置。
 */
export const hasAuthority = (user, authority) => {
  if (!authority || !user) return false;
  return Array.isArray(user.authorities)
    && user.authorities.some(item => item?.name === authority);
};

export const hasAnyAuthority = (user, authorities = []) =>
  authorities.some(authority => hasAuthority(user, authority));

export const ROLE_DEFINITIONS = Object.freeze({
  user: Object.freeze({ key: 'user', label: '用户', title: '用户登录', subtitle: '欢迎回来', button: '登录', icon: 'fas fa-user', target: '/index' }),
  merchant: Object.freeze({ key: 'merchant', label: '商家', title: '商家登录', subtitle: '管理店铺和订单', button: '登录', icon: 'fas fa-store', target: '/merchant/business', applyTarget: '/merchant/apply', authority: 'BUSINESS' }),
  rider: Object.freeze({ key: 'rider', label: '骑手', title: '骑手登录', subtitle: '开始今天的配送', button: '登录', icon: 'fas fa-motorcycle', target: '/rider/dashboard', applyTarget: '/rider/apply', authority: 'RIDER' }),
  admin: Object.freeze({ key: 'admin', label: '管理员', title: '管理员登录', subtitle: '平台管理', button: '登录', icon: 'fas fa-shield-alt', target: '/admin/home', authority: 'ADMIN' })
});

export const getRoleDefinition = key => ROLE_DEFINITIONS[key] || ROLE_DEFINITIONS.user;

export const roleCanEnter = (user, roleKey) => {
  const role = getRoleDefinition(roleKey);
  return !role.authority || hasAuthority(user, role.authority);
};
