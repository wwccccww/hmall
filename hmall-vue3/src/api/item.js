import request from '@/utils/request'

/** 分页查询商品（可传 { silentError: true } 关闭全局错误弹窗，自行处理失败） */
export function getItemPage(params, extraConfig = {}) {
  return request.get('/items/page', { params, ...extraConfig })
}

/** 根据 id 查询商品详情 */
export function getItemById(id, extraConfig = {}) {
  return request.get(`/items/${id}`, extraConfig)
}
