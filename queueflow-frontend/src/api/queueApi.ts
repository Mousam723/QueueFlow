import { apiRequest } from './client'
import type { PageResponse, QueueResponse } from '../types/api'

export async function getQueue(queueId: number): Promise<QueueResponse> {
  return apiRequest<QueueResponse>(`/api/v1/queues/${queueId}`)
}

export async function getBusinessQueues(
  businessId: number,
  date: string,
  page = 0,
  size = 10,
): Promise<PageResponse<QueueResponse>> {
  return apiRequest<PageResponse<QueueResponse>>(
    `/api/v1/businesses/${businessId}/queues?date=${encodeURIComponent(date)}&page=${page}&size=${size}`,
  )
}

export interface CreateQueueRequest {
  serviceId: number
  queueDate?: string
}

export async function createQueue(
  businessId: number,
  request: CreateQueueRequest,
): Promise<QueueResponse> {
  return apiRequest<QueueResponse>(
    `/api/v1/businesses/${businessId}/queues`,
    {
      method: 'POST',
      body: JSON.stringify(request),
    },
  )
}

export async function advanceQueue(
  queueId: number,
): Promise<QueueResponse> {
  return apiRequest<QueueResponse>(
    `/api/v1/queues/${queueId}/next`,
    {
      method: 'POST',
    },
  )
}

export async function pauseQueue(
  queueId: number,
): Promise<QueueResponse> {
  return apiRequest<QueueResponse>(
    `/api/v1/queues/${queueId}/pause`,
    {
      method: 'POST',
    },
  )
}

export async function resumeQueue(
  queueId: number,
): Promise<QueueResponse> {
  return apiRequest<QueueResponse>(
    `/api/v1/queues/${queueId}/resume`,
    {
      method: 'POST',
    },
  )
}

export async function closeQueue(
  queueId: number,
): Promise<QueueResponse> {
  return apiRequest<QueueResponse>(
    `/api/v1/queues/${queueId}/close`,
    {
      method: 'POST',
    },
  )
}