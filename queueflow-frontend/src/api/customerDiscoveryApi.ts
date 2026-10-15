import { apiRequest } from './client'

import type {
  BusinessResponse,
  PageResponse,
  QueueResponse,
  ServiceResponse,
} from '../types/api'

export async function getDiscoverableBusinesses(
  page = 0,
  size = 10,
): Promise<PageResponse<BusinessResponse>> {
  return apiRequest<PageResponse<BusinessResponse>>(
    `/api/v1/customer/businesses?page=${page}&size=${size}`,
  )
}

export async function getDiscoverableServices(
  businessId: number,
  page = 0,
  size = 10,
): Promise<PageResponse<ServiceResponse>> {
  return apiRequest<PageResponse<ServiceResponse>>(
    `/api/v1/customer/businesses/${businessId}/services?page=${page}&size=${size}`,
  )
}

export async function getActiveQueue(
  businessId: number,
  serviceId: number,
  date?: string,
): Promise<QueueResponse> {
  const query = date
    ? `?date=${encodeURIComponent(date)}`
    : ''

  return apiRequest<QueueResponse>(
    `/api/v1/customer/businesses/${businessId}/services/${serviceId}/queues${query}`,
  )
}
