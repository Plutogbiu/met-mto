import { request } from '../utils/request'

export function login(data) {
  return request({
    url: '/app/auth/login',
    method: 'POST',
    data,
  })
}

export function logout() {
  return request({
    url: '/app/auth/logout',
    method: 'POST',
  })
}

export function changePassword(data) {
  return request({
    url: '/app/auth/password',
    method: 'PUT',
    data,
  })
}
