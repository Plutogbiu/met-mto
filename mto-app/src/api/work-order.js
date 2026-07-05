import { request } from '../utils/request'

export function fetchWorkOrders(params) {
  return request({
    url: '/admin/work-orders',
    method: 'GET',
    data: params,
  })
}

export function fetchWorkOrderDetail(id) {
  return request({
    url: `/admin/work-orders/${id}`,
    method: 'GET',
  })
}

export function updateWorkOrderStatus(id, status) {
  return request({
    url: `/admin/work-orders/${id}/status?status=${encodeURIComponent(status)}`,
    method: 'PUT',
  })
}

export function completeWorkOrder(id) {
  return request({
    url: `/admin/work-orders/${id}/complete`,
    method: 'PUT',
  })
}

export function fetchWorkOrderRecords(workOrderId) {
  return request({
    url: `/admin/work-orders/${workOrderId}/records`,
    method: 'GET',
  })
}

export function createWorkOrderRecord(workOrderId, payload) {
  const data = typeof payload === 'string' ? { content: payload } : payload
  return request({
    url: `/admin/work-orders/${workOrderId}/records`,
    method: 'POST',
    contentType: 'application/x-www-form-urlencoded',
    data,
  })
}

export function fetchAttachments(params) {
  return request({
    url: '/admin/attachments',
    method: 'GET',
    data: params,
  })
}
