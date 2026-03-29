import request from '@/utils/request'

/** 登录 */
export function login(data) {
  return request.post('/users/login', data)
}

/** 当前登录用户信息 */
export function getCurrentUser() {
  return request.get('/users/me')
}
