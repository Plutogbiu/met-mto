import { getApiBaseUrl, getFileBaseUrl } from '../config'
import { clearAuth, getToken } from './auth'

function handleApiResult(resolve, reject, response) {
  const result = response?.data
  if (!result || typeof result.code === 'undefined') {
    resolve(result)
    return
  }
  if (result.code === 0) {
    resolve(result.data)
    return
  }
  if (result.code === 401) {
    clearAuth()
    uni.showToast({ title: '登录已过期', icon: 'none' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/login/login' })
    }, 200)
  } else if (result.message) {
    uni.showToast({ title: result.message, icon: 'none' })
  }
  reject(new Error(result.message || '请求失败'))
}

export function request(options = {}) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    uni.request({
      url: `${getApiBaseUrl()}${options.url || ''}`,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': options.contentType || 'application/json',
        ...(options.header || {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      timeout: options.timeout || 15000,
      success: (response) => handleApiResult(resolve, reject, response),
      fail: (error) => {
        uni.showToast({ title: '网络请求失败', icon: 'none' })
        reject(error)
      },
    })
  })
}

export function uploadFile({ url, filePath, name = 'file', formData = {} }) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    uni.uploadFile({
      url: `${getApiBaseUrl()}${url}`,
      filePath,
      name,
      formData,
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (response) => {
        try {
          const data = JSON.parse(response.data || '{}')
          if (data.code === 0) {
            resolve(data.data)
            return
          }
          uni.showToast({ title: data.message || '上传失败', icon: 'none' })
          reject(new Error(data.message || '上传失败'))
        } catch (error) {
          reject(error)
        }
      },
      fail: (error) => {
        uni.showToast({ title: '上传失败', icon: 'none' })
        reject(error)
      },
    })
  })
}

export function fileUrl(path) {
  if (!path) {
    return ''
  }
  if (/^https?:\/\//.test(path)) {
    return path
  }
  return `${getFileBaseUrl()}${path}`
}
