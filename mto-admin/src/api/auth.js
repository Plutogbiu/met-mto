import http from './http'

export function login(data) {
  return http.post('/admin/auth/login', data)
}

export function logout() {
  return http.post('/admin/auth/logout')
}
