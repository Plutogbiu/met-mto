import {
  AUTH_EXPIRE_KEY,
  AUTH_REMEMBER_KEY,
  AUTH_TOKEN_KEY,
  AUTH_USER_KEY,
} from '../config'

export function setAuth(session) {
  if (!session) {
    clearAuth()
    return
  }
  const expiresIn = Number(session.expiresIn || 0)
  const expiresAt = expiresIn > 0 ? Date.now() + expiresIn * 1000 : 0
  uni.setStorageSync(AUTH_TOKEN_KEY, session.token || '')
  uni.setStorageSync(AUTH_USER_KEY, session)
  uni.setStorageSync(AUTH_EXPIRE_KEY, expiresAt)
}

export function getToken() {
  const token = uni.getStorageSync(AUTH_TOKEN_KEY)
  const expiresAt = Number(uni.getStorageSync(AUTH_EXPIRE_KEY) || 0)
  if (expiresAt > 0 && Date.now() > expiresAt) {
    clearAuth()
    return ''
  }
  return token || ''
}

export function getAuthUser() {
  const expiresAt = Number(uni.getStorageSync(AUTH_EXPIRE_KEY) || 0)
  if (expiresAt > 0 && Date.now() > expiresAt) {
    clearAuth()
    return null
  }
  const value = uni.getStorageSync(AUTH_USER_KEY)
  if (typeof value === 'string') {
    try {
      return JSON.parse(value)
    } catch (error) {
      return null
    }
  }
  return value || null
}

export function clearAuth() {
  uni.removeStorageSync(AUTH_TOKEN_KEY)
  uni.removeStorageSync(AUTH_USER_KEY)
  uni.removeStorageSync(AUTH_EXPIRE_KEY)
}

export function getRememberedCredentials() {
  const value = uni.getStorageSync(AUTH_REMEMBER_KEY)
  const credentials = typeof value === 'string' ? parseRememberedCredentials(value) : value
  if (!credentials?.username || !credentials?.password) {
    clearRememberedCredentials()
    return null
  }
  return {
    username: credentials.username,
    password: credentials.password,
  }
}

export function saveRememberedCredentials(credentials) {
  if (!credentials?.username || !credentials?.password) {
    return
  }
  uni.setStorageSync(AUTH_REMEMBER_KEY, {
    username: credentials.username,
    password: credentials.password,
  })
}

export function clearRememberedCredentials() {
  uni.removeStorageSync(AUTH_REMEMBER_KEY)
}

function parseRememberedCredentials(value) {
  try {
    return JSON.parse(value)
  } catch (error) {
    return null
  }
}

export function isLoggedIn() {
  return Boolean(getToken())
}
