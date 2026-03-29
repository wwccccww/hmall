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

/** 401：确认后前往登录页（管理端 → /admin-login，商城 → /login） */
function confirmRedirectToLogin() {
  const path = window.location.pathname || ''
  const loginPath = isAdminBackendPath(path) ? '/admin-login' : '/login'
  const back = path + (window.location.search || '')
  const ok = window.confirm('登录已失效，是否前往登录页？')
  if (ok) {
    window.location.assign(
      loginPath + '?redirect=' + encodeURIComponent(back || '/')
    )
  }
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

request.interceptors.request.use(
  config => {
    const raw = sessionStorage.getItem('token')
    const token =
      raw && raw !== 'undefined' && raw !== 'null' ? String(raw).trim() : ''
    if (token) {
      config.headers['authorization'] = token
    } else {
      delete config.headers['authorization']
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

    if (status === 401 && !isLoginRequest && !silent) {
      clearAuthState()
      confirmRedirectToLogin()
      return Promise.reject(err)
    }

    showApiErrorAlert(err, { silent })

    return Promise.reject(err)
  }
)

export default request
