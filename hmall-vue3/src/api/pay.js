import request from '@/utils/request'

/** 创建支付单 */
export function createPayOrder(data) {
  return request.post('/pay-orders', data)
}

/** 发起支付 */
export function payOrder(payOrderNo, data) {
  return request.post(`/pay-orders/${payOrderNo}`, data)
}
