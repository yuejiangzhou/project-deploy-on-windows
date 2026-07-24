import request from '@/utils/request'

export function generateLicense(data) {
  return request.post('/license/generate', data)
}

export function renewLicense(id, data) {
  return request.post(`/license/${id}/renew`, data)
}

export function getLicenseList(params) {
  return request.get('/license/list', { params })
}

export function getLicenseDetail(id) {
  return request.get(`/license/${id}`)
}

export function getDownloadUrl(id) {
  return `/api/license/${id}/download`
}
