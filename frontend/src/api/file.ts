import http from './http'

export interface UploadedFileResponse {
  fileName: string
  storedName: string
  filePath: string
  fileSize: number
  contentType: string
}

export function uploadFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)

  return http.post<UploadedFileResponse, UploadedFileResponse>('/files/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 下载/预览附件，返回二进制 Blob。
 * disposition: 'inline' 浏览器内预览，'attachment' 触发下载
 */
export function fetchAttachment(id: number, disposition: 'inline' | 'attachment' = 'attachment') {
  return http.get<Blob, Blob>(`/files/${id}`, {
    params: { disposition },
    responseType: 'blob'
  })
}
