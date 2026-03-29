import request from '@/utils/request'

/** 查询所有进行中的优惠券列表（公开，无需登录） */
export const getAvailableCoupons = () =>
  request.get('/coupons')

/** 查询当前登录用户的领券记录（需登录） */
export const getMyCoupons = () =>
  request.get('/coupons/my')

/** 用户抢券（需登录），id 为优惠券 ID */
export const receiveCoupon = (id) =>
  request.post(`/coupons/${id}/receive`)

/** 管理端：创建优惠券 */
export const createCoupon = (data) =>
  request.post('/coupons', data)

/** 管理端：发布优惠券（将库存同步到 Redis） */
export const publishCoupon = (id) =>
  request.put(`/coupons/${id}/publish`)
