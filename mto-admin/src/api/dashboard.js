import http from './http'

export function getDashboardOverview(params) {
  return http.get('/admin/dashboard/overview', { params })
}
