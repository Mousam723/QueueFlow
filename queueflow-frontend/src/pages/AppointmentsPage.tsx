import { useEffect, useState } from 'react'
import { ApiError } from '../api/client'
import {
  callAppointment,
  cancelAppointment,
  completeAppointment,
  getAppointmentPosition,
  getBusinessAppointments,
  getCustomerAppointments,
  markAppointmentNoShow,
  startAppointment,
} from '../api/appointmentApi'
import type {
  AppointmentResponse,
  AppointmentStatus,
  BusinessResponse,
  QueuePositionResponse,
} from '../types/api'

import { getMyBusinesses } from '../api/businessApi'
import { useAuth } from '../auth/useAuth'

function getStatusClass(status: AppointmentStatus) {
  return `appointment-status appointment-status-${status.toLowerCase()}`
}

function AppointmentsPage() {
  const { user } = useAuth()
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([])
  const [businesses, setBusinesses] = useState<BusinessResponse[]>([])
  const [selectedBusinessId, setSelectedBusinessId] = useState<number | null>(
    null,
  )
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [actingAppointmentId, setActingAppointmentId] = useState<number | null>(
    null,
  )
  const [position, setPosition] = useState<QueuePositionResponse | null>(null)

  const isCustomer = user?.role === 'CUSTOMER'
  const isBusinessUser =
    user?.role === 'BUSINESS_OWNER' ||
    user?.role === 'STAFF' ||
    user?.role === 'ADMIN'

  async function loadAppointments() {
    setLoading(true)
    setError(null)

    try {
      if (isCustomer && user) {
        const result = await getCustomerAppointments(user.userId)
        setAppointments(result.content)
        return
      }

      if (isBusinessUser && selectedBusinessId !== null) {
        const result = await getBusinessAppointments(selectedBusinessId)
        setAppointments(result.content)
        return
      }

      setAppointments([])
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError('Unable to load appointments.')
      }
      setAppointments([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let cancelled = false

    async function fetchAppointments() {
      setLoading(true)
      setError(null)

      try {
        if (isCustomer && user) {
          const result = await getCustomerAppointments(user.userId)

          if (!cancelled) {
            setAppointments(result.content)
          }
          return
        }

        if (isBusinessUser) {
          const businessResult = await getMyBusinesses()
          if (!cancelled) {
            setBusinesses(businessResult.content)
            const firstBusinessId = businessResult.content[0]?.id ?? null
            setSelectedBusinessId(firstBusinessId)

            if (firstBusinessId === null) {
              setAppointments([])
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
            setError('Unable to load appointments.')
          }
          setAppointments([])
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    void fetchAppointments()

    return () => {
      cancelled = true
    }
  }, [isCustomer, isBusinessUser, user])

  useEffect(() => {
    if (!isBusinessUser || selectedBusinessId === null) {
      return
    }

    const businessId = selectedBusinessId

    let cancelled = false

    async function fetchBusinessAppointments() {
      setLoading(true)
      setError(null)
      setPosition(null)

      try {
        const result = await getBusinessAppointments(businessId)

        if (!cancelled) {
          setAppointments(result.content)
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
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    void fetchBusinessAppointments()

    return () => {
      cancelled = true
    }
  }, [isBusinessUser, selectedBusinessId])

  async function handlePosition(appointmentId: number) {
    setActionError(null)

    try {
      const result = await getAppointmentPosition(appointmentId)
      setPosition(result)
    } catch (err) {
      if (err instanceof ApiError) {
        setActionError(err.message)
      } else {
        setActionError('Unable to load queue position.')
      }
    }
  }

  async function handleAction(
    appointmentId: number,
    action: () => Promise<AppointmentResponse>,
  ) {
    setActingAppointmentId(appointmentId)
    setActionError(null)
    setPosition(null)

    try {
      const updatedAppointment = await action()

      setAppointments((current) =>
        current.map((appointment) =>
          appointment.id === updatedAppointment.id
            ? updatedAppointment
            : appointment,
        ),
      )
    } catch (err) {
      if (err instanceof ApiError) {
        setActionError(err.message)
      } else {
        setActionError('Unable to update appointment.')
      }
    } finally {
      setActingAppointmentId(null)
    }
  }

  return (
    <section>
      <div className="page-header">
        <p className="eyebrow">APPOINTMENT MANAGEMENT</p>

        <h1>Appointments</h1>

        <p className="page-description">
          {isCustomer
            ? 'View your appointments and queue status.'
            : 'Manage business appointments and their lifecycle.'}
        </p>
      </div>

      <div className="appointment-toolbar">
        <div>
          <span className="card-label">
            {isCustomer ? 'Customer' : 'Business'}
          </span>

          {isCustomer && user && (
            <strong className="toolbar-value">
              {user.name}
            </strong>
          )}

          {isBusinessUser && (
            <select
              className="form-select"
              value={selectedBusinessId ?? ''}
              onChange={(event) =>
                setSelectedBusinessId(
                  event.target.value ? Number(event.target.value) : null,
                )
              }
              disabled={businesses.length === 0 || loading}
            >
              {businesses.length === 0 && (
                <option value="">No businesses available</option>
              )}

              {businesses.map((business) => (
                <option key={business.id} value={business.id}>
                  {business.name} (#{business.id})
                </option>
              ))}
            </select>
          )}
        </div>

        <button
          className="secondary-button"
          type="button"
          onClick={() => void loadAppointments()}
          disabled={loading}
        >
          Refresh
        </button>
      </div>
      {loading && (
        <div className="empty-state">
          <h2>Loading appointments...</h2>
          <p>Fetching appointment data from the backend.</p>
        </div>
      )}

      {!loading && error && (
        <div className="empty-state">
          <h2>Unable to load appointments</h2>
          <p>{error}</p>
        </div>
      )}

      {!loading && !error && appointments.length === 0 && (
        <div className="empty-state">
          <div className="empty-state-icon">A</div>

          <h2>No appointments found</h2>

          <p>
            {isCustomer
              ? 'You do not have any appointments yet.'
              : 'This business does not have any appointments yet.'}
          </p>
        </div>
      )}

      {!loading && !error && appointments.length > 0 && (
        <div className="appointment-list">
          {appointments.map((appointment) => {
            const isActing = actingAppointmentId === appointment.id

            return (
              <article
                className="appointment-card"
                key={appointment.id}
              >
                <div className="appointment-card-header">
                  <div>
                    <span className="card-label">
                      Appointment #{appointment.id}
                    </span>

                    <h2>
                      Token #{appointment.tokenNumber}
                    </h2>
                  </div>

                  <span className={getStatusClass(appointment.status)}>
                    {appointment.status.replace('_', ' ')}
                  </span>
                </div>

                <div className="appointment-details">
                  <div>
                    <span className="card-label">Queue</span>
                    <strong>#{appointment.queueId}</strong>
                  </div>

                  <div>
                    <span className="card-label">Service</span>
                    <strong>#{appointment.serviceId}</strong>
                  </div>

                  <div>
                    <span className="card-label">Date</span>
                    <strong>{appointment.appointmentDate}</strong>
                  </div>
                </div>

                {actionError && actingAppointmentId === null && (
                  <div className="appointment-error">
                    {actionError}
                  </div>
                )}

                {position?.appointmentId === appointment.id && (
                  <div className="appointment-position">
                    <div>
                      <span>Queue position</span>
                      <strong>{position.queuePosition}</strong>
                    </div>
                    <div>
                      <span>Estimated wait</span>
                      <strong>
                        {position.estimatedWaitMinutes} min
                      </strong>
                    </div>
                  </div>
                )}

                <div className="appointment-actions">
                  {isBusinessUser && appointment.status === 'WAITING' && (
                    <button
                      className="primary-button"
                      type="button"
                      disabled={isActing}
                      onClick={() =>
                        void handleAction(
                          appointment.id,
                          () => callAppointment(appointment.id),
                        )
                      }
                    >
                      {isActing ? 'Updating...' : 'Call'}
                    </button>
                  )}

                  {isBusinessUser && appointment.status === 'CALLED' && (
                    <button
                      className="primary-button"
                      type="button"
                      disabled={isActing}
                      onClick={() =>
                        void handleAction(
                          appointment.id,
                          () => startAppointment(appointment.id),
                        )
                      }
                    >
                      {isActing ? 'Updating...' : 'Start'}
                    </button>
                  )}

                  {isBusinessUser && appointment.status === 'IN_SERVICE' && (
                    <button
                      className="primary-button"
                      type="button"
                      disabled={isActing}
                      onClick={() =>
                        void handleAction(
                          appointment.id,
                          () => completeAppointment(appointment.id),
                        )
                      }
                    >
                      {isActing ? 'Updating...' : 'Complete'}
                    </button>
                  )}

                  {isBusinessUser &&
                    (appointment.status === 'WAITING' ||
                      appointment.status === 'CALLED') && (
                    <>
                      <button
                        className="secondary-button"
                        type="button"
                        disabled={isActing}
                        onClick={() =>
                          void handleAction(
                            appointment.id,
                            () => markAppointmentNoShow(appointment.id),
                          )
                        }
                      >
                        No-show
                      </button>
                      <button
                        className="danger-button"
                        type="button"
                        disabled={isActing}
                        onClick={() =>
                          void handleAction(
                            appointment.id,
                            () => cancelAppointment(appointment.id),
                          )
                        }
                      >
                        Cancel
                      </button>
                    </>
                  )}

                  {isCustomer &&
                    (appointment.status === 'WAITING' ||
                      appointment.status === 'CALLED') && (
                    <button
                      className="danger-button"
                      type="button"
                      disabled={isActing}
                      onClick={() =>
                        void handleAction(
                          appointment.id,
                          () => cancelAppointment(appointment.id),
                        )
                      }
                    >
                      {isActing ? 'Cancelling...' : 'Cancel appointment'}
                    </button>
                  )}

                  {appointment.status === 'WAITING' && (
                    <button
                      className="secondary-button"
                      type="button"
                      disabled={isActing}
                      onClick={() =>
                        void handlePosition(appointment.id)
                      }
                    >
                      Position
                    </button>
                  )}
                </div>
              </article>
            )
          })}
        </div>
      )}
    </section>
  )
}

export default AppointmentsPage
