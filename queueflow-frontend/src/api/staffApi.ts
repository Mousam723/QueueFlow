import { apiRequest } from './client'

import type {
  PageResponse,
  StaffResponse,
} from '../types/api'

export interface AssignStaffRequest {
  staffId: number
}

export async function getAvailableStaff(
  businessId: number,
  page = 0,
  size = 10,
): Promise<PageResponse<StaffResponse>> {
  return apiRequest<PageResponse<StaffResponse>>(
    `/api/v1/businesses/${businessId}/staff/available?page=${page}&size=${size}`,
  )
}

export async function getBusinessStaff(
  businessId: number,
  page = 0,
  size = 10,
): Promise<PageResponse<StaffResponse>> {
  return apiRequest<PageResponse<StaffResponse>>(
    `/api/v1/businesses/${businessId}/staff?page=${page}&size=${size}`,
  )
}

export async function assignStaff(
  businessId: number,
  request: AssignStaffRequest,
): Promise<StaffResponse> {
  return apiRequest<StaffResponse>(
    `/api/v1/businesses/${businessId}/staff`,
    {
      method: 'POST',
      body: JSON.stringify(request),
    },
  )
}
