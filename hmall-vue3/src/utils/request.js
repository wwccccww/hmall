import axios from 'axios'
import { getActivePinia } from 'pinia'
import { useUserStore } from '../store/user'
import { showApiErrorAlert } from './apiError'

function clearAuthState() {
  const pinia = getActivePinia()
  if (pinia) {
    useUserStore(pinia).clearUserInfo()
  } else {
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('user-info')
  }
}

/** 是否为后台管理区（含 /admin、/admin/dashboard 等；不含 /admin-login 登录页本身） */
function isAdminBackendPath(path) {
  if (!path) return false
  if (path === '/admin-login') return false
  return path === '/admin' || path.startsWith('/admin/')
}

/** 并发 401 只处理一次，避免多次 alert/跳转 */
let redirectingToLogin = false

/** 401：alert 点确定后必跳登录页（管理端 → /admin-login，商城 → /login） */
function alertRedirectToLogin() {
  if (redirectingToLogin) return
  redirectingToLogin = true
  const path = window.location.pathname || ''
  const loginPath = isAdminBackendPath(path) ? '/admin-login' : '/login'
  const back = path + (window.location.search || '')
  window.alert('登录已失效，请重新登录')
  window.location.assign(loginPath + '?redirect=' + encodeURIComponent(back || '/'))
}

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    // 避免 Spring/Tomcat 按 text/html 返回错误页，便于解析 JSON（含 R.msg）
    Accept: 'application/json'
  }
})

export {
  getServerErrorMessage,
  getApiErrorDescription,
  showApiErrorAlert
} from './apiError'

/** 登录态下供下游解析 UserContext（与网关 JWT 写入的 user-info 一致；直连微服务时浏览器需自带） */
function userInfoIdFromStorage() {
  try {
    const raw = sessionStorage.getItem('user-info')
    if (!raw) return ''
    const u = JSON.parse(raw)
    if (u && u.userId != null && u.userId !== '') return String(u.userId)
  } catch {
    /* ignore */
  }
  return ''
}

request.interceptors.request.use(
  config => {
    const raw = sessionStorage.getItem('token')
    const token =
      raw && raw !== 'undefined' && raw !== 'null' ? String(raw).trim() : ''
    if (token) {
      config.headers['authorization'] = token
      const uid = userInfoIdFromStorage()
      if (uid) {
        config.headers['user-info'] = uid
      } else {
        delete config.headers['user-info']
      }
    } else {
      delete config.headers['authorization']
      delete config.headers['user-info']
    }
    return config
  }
)

request.interceptors.response.use(
  response => response.data,
  err => {
    const status = err.response?.status
    const url = err.config?.url || ''
    const isLoginRequest = url.includes('/users/login')
    const silent = err.config?.silentError === true

    // 401 不受 silentError 影响：必须清理登录态并提示后跳转
    if (status === 401 && !isLoginRequest) {
      clearAuthState()
      alertRedirectToLogin()
      return Promise.reject(err)
    }

    showApiErrorAlert(err, { silent })

    return Promise.reject(err)
  }
)

export default request
