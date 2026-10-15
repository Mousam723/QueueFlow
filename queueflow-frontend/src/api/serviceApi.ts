import { apiRequest } from './client'

import type {
  PageResponse,
  ServiceResponse,
} from '../types/api'

export interface CreateServiceRequest {
  name: string
  description?: string
  durationMinutes: number
}

export async function getBusinessServices(
  businessId: number,
  page = 0,
  size = 10,
): Promise<PageResponse<ServiceResponse>> {
  return apiRequest<PageResponse<ServiceResponse>>(
    `/api/v1/businesses/${businessId}/services?page=${page}&size=${size}`,
  )
}

export async function createBusinessService(
  businessId: number,
  request: CreateServiceRequest,
): Promise<ServiceResponse> {
  return apiRequest<ServiceResponse>(
    `/api/v1/businesses/${businessId}/services`,
    {
      method: 'POST',
      body: JSON.stringify(request),
    },
  )
}

export async function deactivateBusinessService(
  businessId: number,
  serviceId: number,
): Promise<ServiceResponse> {
  return apiRequest<ServiceResponse>(
    `/api/v1/businesses/${businessId}/services/${serviceId}/deactivate`,
    {
      method: 'PATCH',
    },
  )
}

export async function reactivateBusinessService(
  businessId: number,
  serviceId: number,
): Promise<ServiceResponse> {
  return apiRequest<ServiceResponse>(
    `/api/v1/businesses/${businessId}/services/${serviceId}/reactivate`,
    {
      method: 'PATCH',
    },
  )
}
