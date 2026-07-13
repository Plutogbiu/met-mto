export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://43.143.235.157:8082/api'
export const FILE_BASE_URL = API_BASE_URL.replace(/\/api$/, '')

export const AUTH_TOKEN_KEY = 'mto_auth_token'
export const AUTH_USER_KEY = 'mto_auth_user'
export const AUTH_EXPIRE_KEY = 'mto_auth_expires_at'
