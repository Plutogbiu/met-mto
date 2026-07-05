import http from './http'

export function getDevicePage(params) {
  return http.get('/admin/devices', { params })
}

export function createDevice(data) {
  return http.post('/admin/devices', data)
}

export function updateDevice(id, data) {
  return http.put(`/admin/devices/${id}`, data)
}

export function updateDeviceStatus(id, status) {
  return http.put(`/admin/devices/${id}/status`, null, {
    params: { status },
  })
}
