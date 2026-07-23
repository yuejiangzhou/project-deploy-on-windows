import request from '@/utils/request'

export function getProjectList(params) {
  return request.get('/projects', { params })
}

export function getProjectStats() {
  return request.get('/projects/stats')
}

export function getProject(id) {
  return request.get(`/projects/${id}`)
}

export function createProject(data) {
  return request.post('/projects', data)
}

export function updateProject(id, data) {
  return request.put(`/projects/${id}`, data)
}

export function deleteProject(id) {
  return request.delete(`/projects/${id}`)
}

export function getProjectConfig(id) {
  return request.get(`/projects/${id}/config`)
}

export function saveProjectConfig(id, data) {
  return request.post(`/projects/${id}/config`, data)
}

export function getLatestPackage(id) {
  return request.get(`/projects/${id}/latest-package`)
}

export function downloadLatestPackageUrl(id) {
  return `/api/projects/${id}/latest-package/download`
}
