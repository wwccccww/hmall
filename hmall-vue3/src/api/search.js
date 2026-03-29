import request from '@/utils/request'

/** 商品搜索（分页）（可传 { silentError: true } 关闭全局错误弹窗） */
export function getSearchList(params, extraConfig = {}) {
  return request.get('/search/list', { params, ...extraConfig })
}
