import request from '@/utils/request'

export function startPackage(data) {
  return request.post('/package/start', data)
}

export function getPackageProgress(taskId) {
  return request.get(`/package/progress/${taskId}`)
}

export function getPackages(params) {
  return request.get('/packages', { params })
}

export function deletePackage(id) {
  return request.delete(`/packages/${id}`)
}

export function getDownloadUrl(id) {
  return `/api/packages/${id}/download`
}
