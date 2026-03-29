import request from '@/utils/request'

/** 创建订单 */
export function createOrder(data) {
  return request.post('/orders', data)
}

/** 根据 id 查询订单 */
export function getOrderById(orderId) {
  return request.get(`/orders/${orderId}`)
}
