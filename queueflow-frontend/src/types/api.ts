export type QueueStatus =
  | 'ACTIVE'
  | 'PAUSED'
  | 'CLOSED'

export type AppointmentStatus =
  | 'WAITING'
  | 'CALLED'
  | 'IN_SERVICE'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'NO_SHOW'

export interface QueueResponse {
  id: number
  businessId: number
  serviceId: number
  queueDate: string
  status: QueueStatus
  currentToken: number
}

export interface AppointmentResponse {
  id: number
  customerId: number
  businessId: number
  serviceId: number
  queueId: number
  tokenNumber: number
  appointmentDate: string
  status: AppointmentStatus
}

export interface QueuePositionResponse {
  appointmentId: number
  tokenNumber: number
  status: AppointmentStatus
  queuePosition: number
  estimatedWaitMinutes: number
}

export interface ApiErrorResponse {
  status?: number
  message?: string
  error?: string
  timestamp?: string
  path?: string
}
export type ServiceStatus =
  | 'ACTIVE'
  | 'INACTIVE'

export interface ServiceResponse {
  id: number
  businessId: number
  name: string
  description?: string
  durationMinutes: number
  status: ServiceStatus
}

export interface StaffResponse {
  id: number | null
  staffId: number
  name: string
  email: string
  phone?: string
}

export type UserRole = 'CUSTOMER' | 'BUSINESS_OWNER' | 'STAFF' | 'ADMIN'

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  userId: number
  name: string
  email: string
  role: UserRole
}

export interface RegisterRequest {
  name: string
  email: string
  password: string
  phone?: string
  role: UserRole
}

export interface RegisterResponse {
  userId: number
  name: string
  email: string
  phone?: string
  role: UserRole
}

export interface AuthUser {
  userId: number
  name: string
  email: string
  role: UserRole
}

export interface BusinessResponse {
  id: number
  name: string
  description?: string
  address?: string
  timezone: string
  status: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}
