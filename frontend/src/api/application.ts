import http from './http'

export interface ApplicationItemRequest {
  materialName: string
  materialCode?: string
  specification?: string
  unit?: string
  quantity: number
  estimatedUnitPrice?: number
  estimatedAmount?: number
  requiredDate?: string
  suggestSupplierCode?: string
  suggestSupplierName?: string
  remark?: string
}

export interface AttachmentInfoRequest {
  id?: number
  fileName: string
  storedName: string
  filePath: string
  fileSize: number
  contentType: string
}

export interface AttachmentInfo {
  id: number
  fileName: string
  storedName: string
  filePath: string
  fileSize: number
  contentType: string
  createdAt: string
}

export interface ApplicationCreateRequest {
  title: string
  departmentId: number
  approverId?: number
  applicationType?: string
  purpose?: string
  budgetAccountCode?: string
  budgetAccountName?: string
  budgetAvailableAmount?: number
  currency?: string
  requiredDate?: string
  items: ApplicationItemRequest[]
  attachments?: AttachmentInfoRequest[]
}

export interface ApplicationResponse {
  id: number
  applicationNo: string
  title: string
  applicantId: number
  applicantName?: string
  departmentId: number
  departmentName?: string
  applicationType: string
  purpose?: string
  totalAmount?: number
  currency: string
  requiredDate?: string
  status: string
  approvalComment?: string
  submittedAt?: string
  createdAt: string
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export function createApplication(data: ApplicationCreateRequest) {
  return http.post<ApplicationResponse, ApplicationResponse>('/applications', data)
}

export function updateApplication(id: number, data: ApplicationCreateRequest) {
  return http.put<ApplicationResponse, ApplicationResponse>(`/applications/${id}`, data)
}

export function listApplications(
  page = 0,
  size = 20,
  params?: { keyword?: string; status?: string; supplierSelected?: boolean }
) {
  return http.get<PageResult<ApplicationResponse>, PageResult<ApplicationResponse>>('/applications', {
    params: { page, size, ...params }
  })
}

export interface ApplicationDetailItem {
  id: number
  materialCode?: string
  materialName: string
  specification?: string
  unit?: string
  quantity: number
  estimatedUnitPrice?: number
  estimatedAmount?: number
  remark?: string
}

export interface ApplicationDetailResponse {
  id: number
  applicationNo: string
  title: string
  applicantId: number
  approverId?: number
  departmentId: number
  applicationType: string
  purpose?: string
  totalAmount?: number
  currency: string
  requiredDate?: string
  status: string
  approvalComment?: string
  submittedAt?: string
  createdAt: string
  items: ApplicationDetailItem[]
  attachments: AttachmentInfo[]
}

export function getApplication(id: number) {
  return http.get<ApplicationDetailResponse, ApplicationDetailResponse>(`/applications/${id}`)
}

export function submitApplication(id: number) {
  return http.post<ApplicationResponse, ApplicationResponse>(`/applications/${id}/submit`)
}

export function approveApplication(id: number, data?: { comment?: string }) {
  return http.post<ApplicationResponse, ApplicationResponse>(`/applications/${id}/approve`, data ?? {})
}

export function rejectApplication(id: number, data: { comment: string }) {
  return http.post<ApplicationResponse, ApplicationResponse>(`/applications/${id}/reject`, data)
}

export function deleteApplication(id: number) {
  return http.delete<void, void>(`/applications/${id}`)
}

export function reapplyApplication(id: number) {
  return http.post<ApplicationResponse, ApplicationResponse>(`/applications/${id}/reapply`)
}

// ── 申请单候选供应商（询价名单，手动录入） ────────────────────────

export interface ApplicationSupplierResponse {
  id: number
  applicationId: number
  supplierName: string
  contactPerson?: string
  contactPhone?: string
  quotedAmount?: number
  deliveryDays?: number
  selected: boolean
  remark?: string
  createdAt: string
}

export interface ApplicationSupplierRequest {
  supplierName: string
  contactPerson?: string
  contactPhone?: string
  quotedAmount?: number
  deliveryDays?: number
  remark?: string
}

export function listApplicationSuppliers(applicationId: number) {
  return http.get<ApplicationSupplierResponse[], ApplicationSupplierResponse[]>(
    `/applications/${applicationId}/suppliers`
  )
}

export function addApplicationSupplier(applicationId: number, data: ApplicationSupplierRequest) {
  return http.post<ApplicationSupplierResponse, ApplicationSupplierResponse>(
    `/applications/${applicationId}/suppliers`,
    data
  )
}

export function updateApplicationSupplier(
  applicationId: number,
  candidateId: number,
  data: ApplicationSupplierRequest
) {
  return http.put<ApplicationSupplierResponse, ApplicationSupplierResponse>(
    `/applications/${applicationId}/suppliers/${candidateId}`,
    data
  )
}

export function deleteApplicationSupplier(applicationId: number, candidateId: number) {
  return http.delete<void, void>(
    `/applications/${applicationId}/suppliers/${candidateId}`
  )
}

/** 采购负责人/管理员选定中标供应商 */
export function selectApplicationSupplier(applicationId: number, candidateId: number) {
  return http.put<void, void>(
    `/applications/${applicationId}/suppliers/${candidateId}/select`
  )
}

/** 采购负责人/管理员取消选定中标供应商 */
export function unselectApplicationSupplier(applicationId: number, candidateId: number) {
  return http.put<void, void>(
    `/applications/${applicationId}/suppliers/${candidateId}/unselect`
  )
}
