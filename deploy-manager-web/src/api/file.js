import request from '@/utils/request'

export function getFileTree(projectId) {
  return request.get('/files/tree', { params: { projectId } })
}

export function listFiles(params) {
  return request.get('/files/list', { params })
}

export function getPathOptions(params) {
  return request.get('/files/paths', { params })
}

export function uploadFile(formData) {
  return request.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadFolder(formData) {
  return request.post('/files/upload-folder', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteFile(id) {
  return request.delete(`/files/${id}`)
}

export function initChunkedUpload(params) {
  return request.post('/files/upload/chunk-init', null, { params })
}

export function uploadChunk(uploadId, chunkIndex, chunk) {
  const formData = new FormData()
  formData.append('chunk', chunk)
  return request.post('/files/upload/chunk', formData, {
    params: { uploadId, chunkIndex },
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function completeChunkedUpload(uploadId) {
  return request.post('/files/upload/chunk-complete', null, { params: { uploadId } })
}

export function abortChunkedUpload(uploadId) {
  return request.delete(`/files/upload/chunk-abort/${uploadId}`)
}

export async function uploadLargeFile(file, params, onProgress) {
  const CHUNK_SIZE = 10 * 1024 * 1024
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE)
  const initRes = await initChunkedUpload({
    fileName: file.name,
    fileSize: file.size,
    totalChunks,
    projectId: params.projectId,
    type: params.type,
    versionTag: params.versionTag
  })
  const uploadId = initRes?.data?.uploadId || initRes?.uploadId
  if (!uploadId) throw new Error('初始化分片上传失败')

  for (let i = 0; i < totalChunks; i++) {
    const start = i * CHUNK_SIZE
    const end = Math.min(start + CHUNK_SIZE, file.size)
    const chunk = file.slice(start, end)
    await uploadChunk(uploadId, i, chunk)
    if (onProgress) onProgress(Math.round(((i + 1) / totalChunks) * 100))
  }

  return completeChunkedUpload(uploadId)
}
