import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body.code !== 0) {
      if (body.code === 40100) {
        localStorage.removeItem('token')
        localStorage.removeItem('userId')
        localStorage.removeItem('displayName')
        localStorage.removeItem('roleCodes')
        window.location.href = '/login'
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body.data
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('displayName')
      localStorage.removeItem('roleCodes')
      ElMessage.error('登录已失效，请重新登录')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      return Promise.reject(error)
    }

    // 400 校验错误：后端返回 { code, message }，优先展示业务错误信息
    const businessMsg = error.response?.data?.message
    if (businessMsg) {
      ElMessage.error(businessMsg)
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default http
