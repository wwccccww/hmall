import request from '@/utils/request'

/** 当前用户购物车列表（可传 { silentError: true } 关闭全局错误弹窗） */
export function getCarts(extraConfig = {}) {
  return request.get('/carts', extraConfig)
}

/**
 * 加入购物车
 * @param {Record<string, unknown>} data itemId, num, name, price, image, spec 等
 */
export function addCartItem(data) {
  return request.post('/carts', data)
}

/** 更新购物车条目数量 */
export function updateCartItem(data) {
  return request.put('/carts', data)
}

/** 删除购物车条目 */
export function deleteCartItem(id) {
  return request.delete(`/carts/${id}`)
}
