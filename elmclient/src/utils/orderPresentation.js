export const ORDER_STATUS = Object.freeze({
  WAITING_PAYMENT: 0, WAITING_MERCHANT_ACCEPT: 1, WAITING_DISPATCH: 2,
  WAITING_RIDER_ACCEPT: 3, WAITING_PICKUP: 4, DELIVERING: 5,
  DELIVERED: 6, COMPLETED: 7, CANCELLED: 8, DELIVERY_EXCEPTION: 9
});

const BASE_STATUS_TEXT = Object.freeze({
  0: '待支付', 1: '待商家接单', 2: '商家备餐中', 3: '待骑手接单',
  5: '配送中', 6: '已送达·待确认', 7: '已完成', 8: '已取消', 9: '配送异常'
});

export const isPickupOrder = order => order?.serviceMode === 'PICKUP';

export const orderStatusText = (state, order = {}, audience = 'customer') => {
  const value = Number(state);
  if (value === ORDER_STATUS.WAITING_PICKUP) {
    if (isPickupOrder(order)) return audience === 'merchant' ? '待顾客自取' : '待到店自取';
    return audience === 'merchant' ? '待骑手取餐' : '骑手待取餐';
  }
  return BASE_STATUS_TEXT[value] || '未知状态';
};

export const orderStatusClass = state => {
  const value = Number(state);
  if (value === ORDER_STATUS.COMPLETED) return 'completed';
  if ([ORDER_STATUS.CANCELLED, ORDER_STATUS.DELIVERY_EXCEPTION].includes(value)) return 'cancelled';
  if ([ORDER_STATUS.DELIVERING, ORDER_STATUS.DELIVERED].includes(value)) return 'delivering';
  if ([ORDER_STATUS.WAITING_DISPATCH, ORDER_STATUS.WAITING_RIDER_ACCEPT, ORDER_STATUS.WAITING_PICKUP].includes(value)) return 'processing';
  return 'pending';
};

export const TASK_STATUS_TEXT = Object.freeze({
  WAITING_RIDER: '待接单', ACCEPTED: '前往商家', ARRIVED_STORE: '已到店', DELIVERING: '配送中',
  DELIVERED: '已送达', COMPLETED: '已完成', EXCEPTION: '异常处理中', CANCELLED: '已取消'
});

export const taskStatusText = value => TASK_STATUS_TEXT[value] || value || '未知状态';
