import http from './http'

export function getMapConfig() {
  return http.get('/admin/maps/config')
}

export function getPlaceSuggestions(params) {
  return http.get('/admin/maps/place-suggestions', { params })
}
