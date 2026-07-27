import http from './http'

export function getWorkOrderPage(params) {
  return http.get('/admin/work-orders', { params })
}

export function getWorkOrder(id) {
  return http.get(`/admin/work-orders/${id}`)
}

export function createWorkOrder(data) {
  return http.post('/admin/work-orders', data)
}

export function updateWorkOrder(id, data) {
  return http.put(`/admin/work-orders/${id}`, data)
}

export function updateWorkOrderStatus(id, status) {
  return http.put(`/admin/work-orders/${id}/status`, null, {
    params: { status },
  })
}

export function voidWorkOrder(id, reason) {
  return http.put(`/admin/work-orders/${id}/void`, null, {
    params: { reason },
  })
}

export function deleteWorkOrder(id) {
  return http.delete(`/admin/work-orders/${id}`)
}

export function getWorkOrderRecords(id) {
  return http.get(`/admin/work-orders/${id}/records`)
}

export function downloadWorkOrderReceipt(id) {
  return http.get(`/admin/work-orders/${id}/receipt-pdf`, {
    responseType: 'blob',
  })
}

export function createWorkOrderExportTask(data) {
  return http.post('/admin/work-orders/export-tasks', data)
}

export function getWorkOrderExportTask(id) {
  return http.get(`/admin/work-orders/export-tasks/${id}`)
}

export function downloadWorkOrderExportTask(id) {
  return http.get(`/admin/work-orders/export-tasks/${id}/download`, {
    responseType: 'blob',
  })
}

export function createWorkOrderRecord(id, data) {
  return http.post(`/admin/work-orders/${id}/records`, data)
}
