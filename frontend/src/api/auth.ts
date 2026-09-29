import http from './http'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  displayName: string
  departmentId?: number
  roleIds: number[]
  roleCodes: string[]
}

export function login(data: LoginRequest) {
  return http.post<LoginResponse, LoginResponse>('/auth/login', data)
}

export function logout() {
  return http.post<void, void>('/auth/logout')
}

export function getMe() {
  return http.get<LoginResponse, LoginResponse>('/auth/me')
}
