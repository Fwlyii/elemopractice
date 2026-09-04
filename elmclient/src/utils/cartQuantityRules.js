export const MAX_QUANTITY_PER_ITEM = 999;

const finiteNumber = (value) => {
  if (value === null || value === undefined || value === '') return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
};

/**
 * 顾客端数量上限的唯一计算入口。前端只负责及时提示，后端仍会重新校验。
 */
export const maxCartQuantity = (item = {}) => {
  const stock = finiteNumber(item.stock);
  const purchaseLimit = finiteNumber(item.purchaseLimit);
  const limits = [MAX_QUANTITY_PER_ITEM];
  if (stock !== null && stock >= 0) limits.push(Math.floor(stock));
  if (purchaseLimit !== null && purchaseLimit > 0) limits.push(Math.floor(purchaseLimit));
  return Math.max(0, Math.min(...limits));
};

export const cartQuantityLimitMessage = (item = {}) => {
  const stock = finiteNumber(item.stock);
  const purchaseLimit = finiteNumber(item.purchaseLimit);
  if (purchaseLimit !== null && purchaseLimit > 0
      && (stock === null || purchaseLimit <= stock)) {
    return `该商品每单最多购买 ${Math.floor(purchaseLimit)} 份`;
  }
  if (stock !== null) {
    return stock <= 0
      ? '该商品当前已售罄'
      : `该商品当前最多可购买 ${Math.floor(stock)} 份`;
  }
  return `单件商品每单最多购买 ${MAX_QUANTITY_PER_ITEM} 份`;
};

export const isSoldOut = (item = {}) => {
  const stock = finiteNumber(item.stock);
  return stock !== null && stock <= 0;
};
