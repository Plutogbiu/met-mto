import { request } from '../utils/request'

export function fetchLatestVersion(params) {
  return request({
    url: '/app/version/latest',
    method: 'GET',
    data: params,
  })
}
