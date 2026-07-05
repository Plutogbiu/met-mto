import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('mto-admin-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const result = response.data
    if (result && typeof result.code === 'number' && result.code !== 0) {
      if (result.code === 401) {
        localStorage.removeItem('mto-admin-token')
        localStorage.removeItem('mto-admin-token-expire-at')
        localStorage.removeItem('mto-admin-user')
        window.location.href = '/login'
        return Promise.reject(result)
      }
      ElMessage.error(result.message || '请求失败')
      return Promise.reject(result)
    }
    return result
  },
  (error) => {
    if (error?.response?.status === 401) {
      localStorage.removeItem('mto-admin-token')
      localStorage.removeItem('mto-admin-token-expire-at')
      localStorage.removeItem('mto-admin-user')
      window.location.href = '/login'
      return Promise.reject(error)
    }
    ElMessage.error(error?.response?.data?.message || '网络请求失败')
    return Promise.reject(error)
  },
)

export default http
