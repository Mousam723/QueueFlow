import { apiRequest } from './client'
import type { BusinessResponse, PageResponse } from '../types/api'

export async function getMyBusinesses(
  page = 0,
  size = 10,
): Promise<PageResponse<BusinessResponse>> {
  return apiRequest<PageResponse<BusinessResponse>>(
    `/api/v1/businesses/mine?page=${page}&size=${size}`,
  )
}
