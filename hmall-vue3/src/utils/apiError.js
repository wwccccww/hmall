/**
 * 从后端响应体中取出「业务错误说明」。
 * 与后端 hm-common 的 R 一致：优先使用字段 msg（及常见别名）。
 */
export function getServerErrorMessage(data) {
  if (data == null) return ''

  if (typeof data === 'string') {
    const t = data.trim()
    if (!t) return ''
    if (t.startsWith('<') || t.startsWith('<!')) return ''
    if (t.startsWith('{') || t.startsWith('[')) {
      try {
        const parsed = JSON.parse(t)
        return getServerErrorMessage(parsed)
      } catch {
        /* 非 JSON 纯文本 */
      }
    }
    return t.length > 500 ? t.slice(0, 500) + '…' : t
  }

  if (typeof data !== 'object') return ''

  /** Spring Boot 默认错误 JSON 的占位文案，不能当业务提示展示 */
  const isSpringPlaceholder = (s) => {
    if (!s || typeof s !== 'string') return false
    const t = s.trim().toLowerCase()
    return t === 'internal server error' || t === 'no message available'
  }

  const pick = (obj) => {
    if (!obj || typeof obj !== 'object') return ''
    // 与 hm-common R 一致：msg 为业务文案
    if (obj.msg != null) {
      const s = typeof obj.msg === 'string' ? obj.msg.trim() : String(obj.msg).trim()
      if (s) return s
    }
    if (obj.message != null) {
      const s =
        typeof obj.message === 'string' ? obj.message.trim() : String(obj.message).trim()
      if (s && !isSpringPlaceholder(s)) return s
    }
    if (obj.errorMessage != null) {
      const s =
        typeof obj.errorMessage === 'string'
          ? obj.errorMessage.trim()
          : String(obj.errorMessage).trim()
      if (s) return s
    }
    if (typeof obj.error === 'string') {
      const s = obj.error.trim()
      // 勿把 Spring 默认的 error 当成业务说明（否则会盖住真实原因或误导用户）
      if (s && !isSpringPlaceholder(s)) return s
    }
    return ''
  }

  let out = pick(data)
  if (out) return out

  if (data.data != null && typeof data.data === 'object') {
    out = pick(data.data)
    if (out) return out
  }

  return ''
}

const STATUS_HINTS = {
  400: '请求参数不正确',
  401: '登录已失效，请重新登录',
  403: '没有权限执行此操作',
  404: '请求的资源不存在',
  408: '请求超时',
  409: '数据冲突，请刷新后重试',
  429: '请求过于频繁，请稍后再试',
  500: '服务异常，请稍后重试（若持续出现请查看网关与微服务日志）',
  502: '网关异常，请稍后重试',
  503: '服务暂时不可用，请确认网关与微服务已启动',
  504: '网关超时，请稍后重试'
}

/**
 * 无后端 msg 时的兜底说明（网络/HTTP 等）
 */
function getFallbackErrorDescription(err) {
  const res = err.response
  const status = res?.status
  if (status) {
    const hint = STATUS_HINTS[status]
    const text = (res.statusText && String(res.statusText).trim()) || ''
    if (hint) return `${hint}（HTTP ${status}）`
    return text ? `请求失败：${text}（${status}）` : `请求失败（HTTP ${status}）`
  }

  if (err.code === 'ECONNABORTED') return '请求超时，请检查网络或稍后重试'
  if (err.code === 'ERR_NETWORK' || err.message === 'Network Error') {
    return '网络异常，请检查网络或确认网关（如 localhost:8080）已启动'
  }
  if (!res) return err.message || '请求失败，请检查网络或服务状态'
  return err.message || '请求失败'
}

/**
 * 完整描述：优先后端 R.msg，没有再用兜底
 */
export function getApiErrorDescription(err) {
  const backend = getServerErrorMessage(err.response?.data)
  if (backend) return backend
  return getFallbackErrorDescription(err)
}

/**
 * 弹窗：优先只展示后端返回的 msg；仅当无法解析出任何后端说明时再展示兜底文案
 */
export function showApiErrorAlert(err, options = {}) {
  if (options.silent) return
  const backendMsg = getServerErrorMessage(err.response?.data)
  const text = backendMsg || getFallbackErrorDescription(err)
  if (text) {
    alert(text)
  }
  err._apiErrorAlertShown = true
}
