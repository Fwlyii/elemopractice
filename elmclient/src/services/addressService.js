import request from '@/utils/request';

const unwrap = (response, fallbackMessage) => {
  if (!response?.success) throw new Error(response?.message || fallbackMessage);
  return response.data;
};

export const listMyAddresses = async () => unwrap(
  await request.get('/api/addresses/me'),
  '获取地址列表失败'
);

export const getMyAddress = async id => unwrap(
  await request.get(`/api/addresses/${id}`),
  '获取地址失败'
);

export const createMyAddress = async address => unwrap(
  await request.post('/api/addresses/me', address),
  '新增地址失败'
);

export const updateMyAddress = async (id, address) => unwrap(
  await request.put(`/api/addresses/${id}`, address),
  '更新地址失败'
);

export const removeMyAddress = async id => unwrap(
  await request.delete(`/api/addresses/${id}`),
  '删除地址失败'
);
