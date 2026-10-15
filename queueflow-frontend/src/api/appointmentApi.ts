import { apiRequest } from './client'
import type {
  AppointmentResponse,
  QueuePositionResponse,
  PageResponse,
} from '../types/api'

export async function getCustomerAppointments(
  customerId: number,
  page = 0,
  size = 10,
): Promise<PageResponse<AppointmentResponse>> {
  return apiRequest<PageResponse<AppointmentResponse>>(
    `/api/v1/appointments/customers/${customerId}?page=${page}&size=${size}`,
  )
}

export async function getAppointment(
  appointmentId: number,
): Promise<AppointmentResponse> {
  return apiRequest<AppointmentResponse>(
    `/api/v1/appointments/${appointmentId}`,
  )
}

export async function getAppointmentPosition(
  appointmentId: number,
): Promise<QueuePositionResponse> {
  return apiRequest<QueuePositionResponse>(
    `/api/v1/appointments/${appointmentId}/position`,
  )
}

export async function callAppointment(
  appointmentId: number,
): Promise<AppointmentResponse> {
  return apiRequest<AppointmentResponse>(
    `/api/v1/appointments/${appointmentId}/call`,
    {
      method: 'POST',
    },
  )
}

export async function startAppointment(
  appointmentId: number,
): Promise<AppointmentResponse> {
  return apiRequest<AppointmentResponse>(
    `/api/v1/appointments/${appointmentId}/start`,
    {
      method: 'POST',
    },
  )
}

export async function completeAppointment(
  appointmentId: number,
): Promise<AppointmentResponse> {
  return apiRequest<AppointmentResponse>(
    `/api/v1/appointments/${appointmentId}/complete`,
    {
      method: 'POST',
    },
  )
}

export async function cancelAppointment(
  appointmentId: number,
): Promise<AppointmentResponse> {
  return apiRequest<AppointmentResponse>(
    `/api/v1/appointments/${appointmentId}/cancel`,
    {
      method: 'POST',
    },
  )
}

export async function markAppointmentNoShow(
  appointmentId: number,
): Promise<AppointmentResponse> {
  return apiRequest<AppointmentResponse>(
    `/api/v1/appointments/${appointmentId}/no-show`,
    {
      method: 'POST',
    },
  )
}

export async function getBusinessAppointments(
  businessId: number,
  page = 0,
  size = 10,
): Promise<PageResponse<AppointmentResponse>> {
  return apiRequest<PageResponse<AppointmentResponse>>(
    `/api/v1/appointments/businesses/${businessId}?page=${page}&size=${size}`,
  )
}

export async function createAppointment(
  customerId: number,
  queueId: number,
): Promise<AppointmentResponse> {
  const idempotencyKey = crypto.randomUUID()

  return apiRequest<AppointmentResponse>(
    '/api/v1/appointments',
    {
      method: 'POST',
      headers: {
        'Idempotency-Key': idempotencyKey,
      },
      body: JSON.stringify({
        customerId,
        queueId,
      }),
    },
  )
}
