import request from '@/utils/request'

export function getInfraList(type) {
  return request({
    url: `/infra/${type}/list`,
    method: 'get'
  })
}

export function uploadInfraFile(type, file, params) {
  const formData = new FormData()
  formData.append('file', file)
  if (params?.version) formData.append('version', params.version)
  if (params?.tag) formData.append('tag', params.tag)
  if (params?.initState) formData.append('initState', params.initState)
  if (params?.belongsTo) formData.append('belongsTo', params.belongsTo)
  return request({
    url: `/infra/${type}/upload`,
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadInfraFolder(type, files, folderName, params) {
  const formData = new FormData()
  files.forEach(f => formData.append('file', f))
  formData.append('folderName', folderName)
  if (params?.version) formData.append('version', params.version)
  if (params?.tag) formData.append('tag', params.tag)
  if (params?.initState) formData.append('initState', params.initState)
  if (params?.belongsTo) formData.append('belongsTo', params.belongsTo)
  return request({
    url: `/infra/${type}/upload-folder`,
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadInfraZip(type, zipFile, params) {
  const formData = new FormData()
  formData.append('file', zipFile)
  if (params?.version) formData.append('version', params.version)
  if (params?.tag) formData.append('tag', params.tag)
  if (params?.initState) formData.append('initState', params.initState)
  if (params?.belongsTo) formData.append('belongsTo', params.belongsTo)
  return request({
    url: `/infra/${type}/upload-zip`,
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteInfraFile(type, id) {
  return request({
    url: `/infra/${type}/${id}`,
    method: 'delete'
  })
}

export function editInfraFile(type, id, data) {
  return request({
    url: `/infra/${type}/${id}`,
    method: 'put',
    data
  })
}

export function getLatestInfra(type) {
  return request({
    url: `/infra/${type}/latest`,
    method: 'get'
  })
}
