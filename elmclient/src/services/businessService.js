import request from '@/utils/request';

const unwrapData = (response, fallback) => {
  if (response?.success === false) throw new Error(response.message || '商家数据请求失败');
  return response?.data ?? fallback;
};

export const searchBusinesses = async (keyword, options = {}) => unwrapData(
  await request.get('/api/businesses/search', {
    params: {
      keyword: String(keyword || '').trim(),
      isScore: Boolean(options.isScore),
      isSales: Boolean(options.isSales)
    }
  }),
  []
);

export const listMyBusinesses = async (status = null) => {
  const params = status === null || status === undefined ? {} : { status };
  return unwrapData(await request.get('/api/businesses/merchant', { params }), []);
};
