import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 5000
})

request.interceptors.request.use(
  config => {
    config.headers['authorization'] = sessionStorage.getItem("token")
    return config
  }
)

request.interceptors.response.use(
  response => response.data,
  err => {
    if (err.response && err.response.status === 401) {
      sessionStorage.removeItem("user-info")
      sessionStorage.removeItem("token")
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export default request
