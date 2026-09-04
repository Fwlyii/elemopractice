import request from '@/utils/request';

const ensureSuccess = (response, fallbackMessage) => {
  if (!response?.success) throw new Error(response?.message || fallbackMessage);
  return response.data;
};

export const listCartItems = async businessId => ensureSuccess(
  await request.get('/api/carts/list', { params: { businessId } }),
  '获取购物车失败'
);

export const addCartItem = async (foodId, quantity = 1) => ensureSuccess(
  await request.post('/api/carts/items', { foodId, quantity }),
  '添加商品失败'
);

export const setCartItemQuantity = async (cartId, quantity) => ensureSuccess(
  await request.put(`/api/carts/${cartId}`, null, { params: { quantity } }),
  '更新商品数量失败'
);

export const removeCartItem = async cartId => ensureSuccess(
  await request.delete(`/api/carts/${cartId}`),
  '删除商品失败'
);
