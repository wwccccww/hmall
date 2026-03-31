import request from '@/utils/request'

/** 查询所有进行中的优惠券列表（公开，无需登录） */
export const getAvailableCoupons = (extraConfig = {}) =>
  request.get('/coupons', extraConfig)

/** 管理端：当前管理员创建的优惠券（全部状态，需登录） */
export const getManageCoupons = (extraConfig = {}) =>
  request.get('/coupons/manage', extraConfig)

/**
 * 管理端：分页查询某张券的领取记录（user_coupon）
 * @param {number} id 优惠券 ID
 * @param {object} [extraConfig] axios 配置，可传 params: { pageNo, pageSize }
 */
export const getCouponReceiveRecords = (id, extraConfig = {}) =>
  request.get(`/coupons/${id}/records`, extraConfig)

/** 查询当前登录用户的领券记录（需登录） */
export const getMyCoupons = (extraConfig = {}) =>
  request.get('/coupons/my', extraConfig)

/** 购物车可用券（后端批量试算，返回每张券立减金额） */
export const getAvailableCouponsForCart = (data, extraConfig = {}) =>
  request.post('/coupons/available', data, extraConfig)

/** 用户抢券（需登录），id 为优惠券 ID；可传 { silentError: true } 避免与页面 catch 重复弹窗 */
export const receiveCoupon = (id, extraConfig = {}) =>
  request.post(`/coupons/${id}/receive`, null, extraConfig)

/** 管理端：创建优惠券；可传 { silentError: true } */
export const createCoupon = (data, extraConfig = {}) =>
  request.post('/coupons', data, extraConfig)

/** 管理端：发布优惠券（将库存同步到 Redis）；可传 { silentError: true } */
export const publishCoupon = (id, extraConfig = {}) =>
  request.put(`/coupons/${id}/publish`, null, extraConfig)

/**
 * 批量获取券的 Redis 实时剩余库存（公开，无需登录）
 * @param {number[]} ids 优惠券 ID 数组
 * @returns {Promise<Record<number, number>>} couponId → 剩余库存
 */
export const getRealtimeStock = (ids, extraConfig = {}) =>
  request.get('/coupons/stock', { params: { ids: ids.join(',') }, ...extraConfig })

/** 管理端：查询商品类目列表（item-service），用于创建“指定类目券” */
export const getItemCategories = (extraConfig = {}) =>
  request.get('/items/categories', extraConfig)
