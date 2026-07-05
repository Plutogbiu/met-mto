import http from './http'

export function getCustomerSitePage(params) {
  return http.get('/admin/customer-sites', { params })
}

export function getCustomerSite(id) {
  return http.get(`/admin/customer-sites/${id}`)
}

export function createCustomerSite(data) {
  return http.post('/admin/customer-sites', data)
}

export function updateCustomerSite(id, data) {
  return http.put(`/admin/customer-sites/${id}`, data)
}

export function updateCustomerSiteStatus(id, status) {
  return http.put(`/admin/customer-sites/${id}/status`, null, {
    params: { status },
  })
}
