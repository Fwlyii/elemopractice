import request from '@/utils/request';

const unwrap = (response, fallbackMessage) => {
  if (!response?.success) throw new Error(response?.message || fallbackMessage);
  return response.data;
};

export const listMyCollections = async () => unwrap(
  await request.get('/api/merchant/interaction/collections/me'),
  '获取收藏列表失败'
);

export const getMyInteraction = async merchantId => unwrap(
  await request.get('/api/merchant/interaction/status/me', { params: { merchantId } }),
  '获取互动状态失败'
);

export const updateMyInteraction = async interaction => unwrap(
  await request.post('/api/merchant/interaction/update', interaction),
  '更新互动状态失败'
);
