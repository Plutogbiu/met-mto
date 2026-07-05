import http from './http'

export function getUserPage(params) {
  return http.get('/admin/users', { params })
}

export function createUser(data) {
  return http.post('/admin/users', data)
}

export function updateUser(id, data) {
  return http.put(`/admin/users/${id}`, data)
}

export function updateUserStatus(id, status) {
  return http.put(`/admin/users/${id}/status`, null, {
    params: { status },
  })
}

export function resetUserPassword(id, password) {
  return http.put(`/admin/users/${id}/password`, { password })
}

export function getUserPermissions(id) {
  return http.get(`/admin/users/${id}/permissions`)
}

export function updateUserPermissions(id, permissions) {
  return http.put(`/admin/users/${id}/permissions`, { permissions })
}
