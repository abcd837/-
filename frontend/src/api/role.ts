import http from './http'

export interface RoleResponse {
  id: number
  code: string
  name: string
  description?: string
  status: number
  createdAt: string
}

export interface RoleCreateRequest {
  code: string
  name: string
  description?: string
}

export interface RoleUpdateRequest {
  code: string
  name: string
  description?: string
  status?: number
}

export function listRoles() {
  return http.get<RoleResponse[], RoleResponse[]>('/roles')
}

export function createRole(data: RoleCreateRequest) {
  return http.post<RoleResponse, RoleResponse>('/roles', data)
}

export function updateRole(id: number, data: RoleUpdateRequest) {
  return http.put<RoleResponse, RoleResponse>(`/roles/${id}`, data)
}

export function deleteRole(id: number) {
  return http.delete<void, void>(`/roles/${id}`)
}
