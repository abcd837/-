import http from './http'

export interface DepartmentResponse {
  id: number
  code: string
  name: string
  parentId?: number
  sortOrder?: number
}

export interface DepartmentCreateRequest {
  code: string
  name: string
  parentId?: number
  sortOrder?: number
}

export function listDepartments() {
  return http.get<DepartmentResponse[], DepartmentResponse[]>('/departments')
}

export function createDepartment(data: DepartmentCreateRequest) {
  return http.post<DepartmentResponse, DepartmentResponse>('/departments', data)
}

export function updateDepartment(id: number, data: DepartmentCreateRequest) {
  return http.put<DepartmentResponse, DepartmentResponse>(`/departments/${id}`, data)
}

export function deleteDepartment(id: number) {
  return http.delete<void, void>(`/departments/${id}`)
}
