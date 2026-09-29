import http from './http'

export interface UserResponse {
  id: number
  userNo: string
  username: string
  displayName: string
  departmentId?: number
  email?: string
  mobile?: string
  status: number
  createdAt: string
  roleIds: number[]
}

export interface UserCreateRequest {
  userNo: string
  username: string
  displayName: string
  departmentId?: number
  email?: string
  mobile?: string
  password: string
  status?: number
  roleIds?: number[]
}

export interface UserUpdateRequest {
  userNo: string
  username: string
  displayName: string
  departmentId?: number
  email?: string
  mobile?: string
  password?: string
  status?: number
  roleIds?: number[]
}

export function listApprovers() {
  return http.get<UserResponse[], UserResponse[]>('/users/approvers')
}

export function listUsers() {
  return http.get<UserResponse[], UserResponse[]>('/users')
}

export function createUser(data: UserCreateRequest) {
  return http.post<UserResponse, UserResponse>('/users', data)
}

export function updateUser(id: number, data: UserUpdateRequest) {
  return http.put<UserResponse, UserResponse>(`/users/${id}`, data)
}

export function deleteUser(id: number) {
  return http.delete<void, void>(`/users/${id}`)
}
