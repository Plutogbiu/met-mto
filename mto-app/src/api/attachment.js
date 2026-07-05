import { request, uploadFile } from '../utils/request'

export function fetchAttachments(params) {
  return request({
    url: '/admin/attachments',
    method: 'GET',
    data: params,
  })
}

export function uploadAttachment({ filePath, bizType, bizId, category }) {
  return uploadFile({
    url: '/admin/attachments/upload',
    filePath,
    formData: {
      bizType,
      bizId,
      category,
    },
  })
}

export function deleteAttachment(id) {
  return request({
    url: `/admin/attachments/${id}`,
    method: 'DELETE',
  })
}
