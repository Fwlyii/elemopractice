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
