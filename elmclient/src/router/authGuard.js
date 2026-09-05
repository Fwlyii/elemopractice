import { clearAuth, getAuthRole, getStoredUser, getToken } from '../utils/auth';
import { getRoleDefinition, roleCanEnter } from '../utils/roles';

const RIDER_SHARED_PATHS = new Set(['/myInformation', '/notifications']);

const requiredRole = (route) => {
  if (route.query.role === 'rider' && RIDER_SHARED_PATHS.has(route.path)) return 'rider';
  return typeof route.meta.role === 'string' ? route.meta.role : null;
};

const loginLocation = (route, role) => ({
  path: '/login',
  query: { ...(role ? { role } : {}), redirect: route.fullPath }
});

export const installAuthGuard = (router) => {
  router.beforeEach((route) => {
    const user = getStoredUser();
    const token = getToken();
    const sessionRole = getAuthRole();
    const roleKey = requiredRole(route);

    if (!route.meta.public && (!user || !token || !sessionRole)) {
      if (user || token || sessionRole) clearAuth();
      return loginLocation(route, roleKey);
    }
    if (!roleKey) return true;

    if (sessionRole !== roleKey) {
      const currentRole = getRoleDefinition(sessionRole);
      return roleCanEnter(user, sessionRole)
        ? currentRole.target
        : (currentRole.applyTarget || '/login');
    }

    const role = getRoleDefinition(roleKey);
    if (route.meta.allowApplicant) {
      return roleCanEnter(user, roleKey) ? role.target : true;
    }
    return roleCanEnter(user, roleKey)
      ? true
      : (role.applyTarget || { path: '/login', query: { role: roleKey } });
  });
};
