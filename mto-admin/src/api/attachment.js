import http from './http'

export function getAttachments(params) {
  return http.get('/admin/attachments', { params })
}
