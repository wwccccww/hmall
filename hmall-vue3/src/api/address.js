import request from '@/utils/request'

/** 收货地址列表（可传 { silentError: true }） */
export function getAddresses(extraConfig = {}) {
  return request.get('/addresses', extraConfig)
}
