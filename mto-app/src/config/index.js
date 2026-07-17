export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://43.143.235.157:8082/api'
export const FILE_BASE_URL = API_BASE_URL.replace(/\/api$/, '')
export const API_BASE_URL_KEY = 'mto_api_base_url'

function normalizeApiBaseUrl(value) {
  let baseUrl = String(value || '').trim().replace(/\/+$/, '')
  if (!/^https?:\/\/[^\s/]+(?:\/[^\s]*)?$/i.test(baseUrl)) {
    return ''
  }
  if (!/\/api$/i.test(baseUrl)) {
    baseUrl += '/api'
  }
  return baseUrl
}

export function getApiBaseUrl() {
  try {
    const savedUrl = normalizeApiBaseUrl(uni.getStorageSync(API_BASE_URL_KEY))
    return savedUrl || API_BASE_URL
  } catch (error) {
    return API_BASE_URL
  }
}

export function saveApiBaseUrl(value) {
  const normalizedUrl = normalizeApiBaseUrl(value)
  if (!normalizedUrl) {
    return ''
  }
  uni.setStorageSync(API_BASE_URL_KEY, normalizedUrl)
  return normalizedUrl
}

export function resetApiBaseUrl() {
  uni.removeStorageSync(API_BASE_URL_KEY)
  return API_BASE_URL
}

export function getFileBaseUrl() {
  return getApiBaseUrl().replace(/\/api$/i, '')
}

export const AUTH_TOKEN_KEY = 'mto_auth_token'
export const AUTH_USER_KEY = 'mto_auth_user'
export const AUTH_EXPIRE_KEY = 'mto_auth_expires_at'
