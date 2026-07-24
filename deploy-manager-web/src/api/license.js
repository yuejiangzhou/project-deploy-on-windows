import request from '@/utils/request'

export function generateLicense(data) {
  return request.post('/licenses/generate', data)
}

export function renewLicense(id, data) {
  return request.post(`/licenses/${id}/renew`, data)
}

export function getLicenseList(params) {
  return request.get('/licenses/list', { params })
}

export function getLicenseDetail(id) {
  return request.get(`/licenses/${id}`)
}

export function getDownloadUrl(id) {
  return `/api/licenses/${id}/download`
}
