import axios from 'axios'
import { showApiErrorAlert } from './apiError'

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

    showApiErrorAlert(err, { silent })

    if (status === 401 && !isLoginRequest && !silent) {
      sessionStorage.removeItem('user-info')
      sessionStorage.removeItem('token')
      const back = window.location.pathname + window.location.search
      window.location.href =
        '/login?redirect=' + encodeURIComponent(back || '/')
    }
    return Promise.reject(err)
  }
)

export default request
