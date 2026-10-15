import { useEffect, useState } from 'react'

import { ApiError } from '../api/client'

import {
  getBusinessAppointments,
  getCustomerAppointments,
} from '../api/appointmentApi'

import { getMyBusinesses } from '../api/businessApi'

import { getBusinessQueues } from '../api/queueApi'

import QueueStatusCard from '../components/QueueStatusCard'

import { useAuth } from '../auth/useAuth'

import type { AppointmentResponse } from '../types/api'

interface AppointmentSummary {
  total: number
  waiting: number
  inService: number
  completed: number
}

function getAppointmentSummary(
  appointments: AppointmentResponse[],
): AppointmentSummary {
  return {
    total: appointments.length,
    waiting: appointments.filter(
      (appointment) => appointment.status === 'WAITING',
    ).length,
    inService: appointments.filter(
      (appointment) => appointment.status === 'IN_SERVICE',
    ).length,
    completed: appointments.filter(
      (appointment) => appointment.status === 'COMPLETED',
    ).length,
  }
}

function getLocalDate(): string {
  const date = new Date()
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function DashboardPage() {
  const { user } = useAuth()

  const [appointments, setAppointments] = useState<AppointmentResponse[]>([])
  const [queueId, setQueueId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [queueError, setQueueError] = useState<string | null>(null)

  const isCustomer = user?.role === 'CUSTOMER'

  const isBusinessUser =
    user?.role === 'BUSINESS_OWNER' ||
    user?.role === 'STAFF' ||
    user?.role === 'ADMIN'

  useEffect(() => {
    let cancelled = false

    async function loadDashboard() {
      if (!user) {
        return
      }

      setLoading(true)
      setError(null)
      setQueueError(null)
      setQueueId(null)

      try {
        if (isCustomer) {
          const result = await getCustomerAppointments(user.userId)

          if (!cancelled) {
            setAppointments(result.content)

            const relevantAppointment =
              result.content.find(
                (appointment) =>
                  appointment.status === 'WAITING' ||
                  appointment.status === 'CALLED' ||
                  appointment.status === 'IN_SERVICE',
              ) ?? result.content[0]

            setQueueId(relevantAppointment?.queueId ?? null)
          }

          return
        }

        if (isBusinessUser) {
          const businessResult = await getMyBusinesses()

          if (cancelled) {
            return
          }

          const firstBusinessId = businessResult.content[0]?.id

          if (firstBusinessId === undefined) {
            setAppointments([])
            return
          }

          try {
            const appointmentResult =
              await getBusinessAppointments(firstBusinessId)

            if (!cancelled) {
              setAppointments(appointmentResult.content)
            }
          } catch (err) {
            if (!cancelled) {
              if (err instanceof ApiError) {
                setError(err.message)
              } else {
                setError('Unable to load appointments.')
              }

              setAppointments([])
            }
          }

          try {
            const queueResult = await getBusinessQueues(
              firstBusinessId,
              getLocalDate(),
            )

            if (!cancelled) {
              const relevantQueue =
                queueResult.content.find(
                  (queue) => queue.status === 'ACTIVE',
                ) ?? queueResult.content[0]

              setQueueId(relevantQueue?.id ?? null)
            }
          } catch (err) {
            if (!cancelled) {
              if (err instanceof ApiError) {
                setQueueError(err.message)
              } else {
                setQueueError('Unable to load queue data.')
              }

              setQueueId(null)
            }
          }

          return
        }

        if (!cancelled) {
          setAppointments([])
        }
      } catch (err) {
        if (!cancelled) {
          if (err instanceof ApiError) {
            setError(err.message)
          } else {
            setError('Unable to load dashboard data.')
          }

          setAppointments([])
          setQueueId(null)
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    void loadDashboard()

    return () => {
      cancelled = true
    }
  }, [isBusinessUser, isCustomer, user])

  const summary = getAppointmentSummary(appointments)

  return (
    <section>
      <p className="eyebrow">OVERVIEW</p>

      <h1>Dashboard</h1>

      <p className="page-description">
        {isCustomer
          ? 'View your appointments and queue status.'
          : 'Monitor business queues and appointments from one place.'}
      </p>

      <div className="dashboard-grid">
        {queueId !== null && (
          <QueueStatusCard queueId={queueId} />
        )}

        {queueError && (
          <article className="dashboard-card">
            <span className="card-label">Queue</span>
            <strong className="card-value">—</strong>
            <span className="card-meta">{queueError}</span>
          </article>
        )}

        <article className="dashboard-card">
          <span className="card-label">Appointments</span>

          <strong className="card-value">
            {loading ? '...' : summary.total}
          </strong>

          <span className="card-meta">
            {error ?? 'Total appointments'}
          </span>
        </article>

        <article className="dashboard-card">
          <span className="card-label">Waiting</span>

          <strong className="card-value">
            {loading ? '...' : summary.waiting}
          </strong>

          <span className="card-meta">
            Customers waiting in queue
          </span>
        </article>

        <article className="dashboard-card">
          <span className="card-label">In Service</span>

          <strong className="card-value">
            {loading ? '...' : summary.inService}
          </strong>

          <span className="card-meta">
            Appointments currently in service
          </span>
        </article>

        <article className="dashboard-card">
          <span className="card-label">Completed</span>

          <strong className="card-value">
            {loading ? '...' : summary.completed}
          </strong>

          <span className="card-meta">
            Completed appointments
          </span>
        </article>
      </div>
    </section>
  )
}

export default DashboardPage
