import http from './http'

export interface SupplierResponse {
  id: number
  supplierNo: string
  name: string
  shortName?: string
  contactPerson?: string
  contactPhone?: string
  email?: string
  address?: string
  remark?: string
  applicationId?: number
  applicationNo?: string
  status: number
  createdAt: string
}

export interface SupplierSaveRequest {
  supplierNo: string
  name: string
  shortName?: string
  contactPerson?: string
  contactPhone?: string
  email?: string
  address?: string
  remark?: string
  applicationId?: number
  status?: number
}

export function listSuppliers(keyword?: string) {
  return http.get<SupplierResponse[], SupplierResponse[]>('/suppliers', {
    params: { keyword: keyword || undefined }
  })
}

export function createSupplier(data: SupplierSaveRequest) {
  return http.post<SupplierResponse, SupplierResponse>('/suppliers', data)
}

export function updateSupplier(id: number, data: SupplierSaveRequest) {
  return http.put<SupplierResponse, SupplierResponse>(`/suppliers/${id}`, data)
}

export function deleteSupplier(id: number) {
  return http.delete<void, void>(`/suppliers/${id}`)
}
