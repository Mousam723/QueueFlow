import { useEffect, useState } from 'react'

import { ApiError } from '../api/client'
import { createAppointment } from '../api/appointmentApi'
import {
  getActiveQueue,
  getDiscoverableBusinesses,
  getDiscoverableServices,
} from '../api/customerDiscoveryApi'
import { useAuth } from '../auth/useAuth'
import type {
  BusinessResponse,
  QueueResponse,
  ServiceResponse,
} from '../types/api'

function getLocalDate(): string {
  const date = new Date()
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function ExplorePage() {
  const { user } = useAuth()

  const [businesses, setBusinesses] = useState<BusinessResponse[]>([])
  const [services, setServices] = useState<ServiceResponse[]>([])
  const [selectedBusiness, setSelectedBusiness] =
    useState<BusinessResponse | null>(null)
  const [selectedService, setSelectedService] =
    useState<ServiceResponse | null>(null)
  const [selectedDate, setSelectedDate] = useState(getLocalDate)
  const [queue, setQueue] = useState<QueueResponse | null>(null)
  const [queueNotFound, setQueueNotFound] = useState(false)

  const [loadingBusinesses, setLoadingBusinesses] = useState(true)
  const [loadingServices, setLoadingServices] = useState(false)
  const [loadingQueue, setLoadingQueue] = useState(false)
  const [booking, setBooking] = useState(false)
  const [bookedAppointment, setBookedAppointment] = useState<Awaited<ReturnType<typeof createAppointment>> | null>(null)

  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function loadBusinesses() {
      if (!user || user.role !== 'CUSTOMER') {
        return
      }

      setLoadingBusinesses(true)
      setError(null)

      try {
        const result = await getDiscoverableBusinesses()

        if (!cancelled) {
          setBusinesses(result.content)
        }
      } catch (err) {
        if (!cancelled) {
          if (err instanceof ApiError) {
            setError(err.message)
          } else {
            setError('Unable to load available businesses.')
          }

          setBusinesses([])
        }
      } finally {
        if (!cancelled) {
          setLoadingBusinesses(false)
        }
      }
    }

    void loadBusinesses()

    return () => {
      cancelled = true
    }
  }, [user])

  async function handleBusinessSelect(business: BusinessResponse) {
    setSelectedBusiness(business)
    setSelectedService(null)
    setServices([])
    setQueue(null)
    setBookedAppointment(null)
    setLoadingServices(true)
    setError(null)

    try {
      const result = await getDiscoverableServices(business.id)

      setServices(result.content)
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError('Unable to load services for this business.')
      }

      setServices([])
    } finally {
      setLoadingServices(false)
    }
  }

  async function handleServiceSelect(service: ServiceResponse) {
    if (!selectedBusiness) {
      return
    }

    setSelectedService(service)
    setQueue(null)
    setQueueNotFound(false)
    setBookedAppointment(null)
    setLoadingQueue(true)
    setError(null)

    try {
      const result = await getActiveQueue(
        selectedBusiness.id,
        service.id,
        selectedDate,
      )

      setQueue(result)
      setQueueNotFound(false)
    } catch (err) {
      setQueue(null)

      if (err instanceof ApiError) {
        if (err.status === 404) {
          setQueueNotFound(true)
        } else {
          setError(err.message)
        }
      } else {
        setError('Unable to load queue availability.')
      }
    } finally {
      setLoadingQueue(false)
    }
  }

  async function handleDateChange(
    event: React.ChangeEvent<HTMLInputElement>,
  ) {
    const date = event.target.value

    setSelectedDate(date)
    setBookedAppointment(null)

    if (!selectedBusiness || !selectedService || !date) {
      return
    }

    setQueue(null)
    setQueueNotFound(false)
    setLoadingQueue(true)
    setError(null)

    try {
      const result = await getActiveQueue(
        selectedBusiness.id,
        selectedService.id,
        date,
      )

      setQueue(result)
      setQueueNotFound(false)
    } catch (err) {
      setQueue(null)

      if (err instanceof ApiError) {
        if (err.status === 404) {
          setQueueNotFound(true)
        } else {
          setError(err.message)
        }
      } else {
        setError('Unable to load queue availability.')
      }
    } finally {
      setLoadingQueue(false)
    }
  }

  async function handleBookAppointment() {
    if (!user || !selectedBusiness || !selectedService || !queue) {
      return
    }

    setBooking(true)
    setError(null)

    try {
      const appointment = await createAppointment(
        user.userId,
        queue.id,
      )

      setBookedAppointment(appointment)
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError('Unable to book the appointment.')
      }
    } finally {
      setBooking(false)
    }
  }

  function handleBackToBusinesses() {
    setSelectedBusiness(null)
    setSelectedService(null)
    setServices([])
    setQueue(null)
    setBookedAppointment(null)
    setError(null)
  }

  function handleBackToServices() {
    setSelectedService(null)
    setQueue(null)
    setBookedAppointment(null)
    setError(null)
  }

  return (
    <section>
      <p className="eyebrow">DISCOVER</p>

      <div className="explore-page-header">
        <div>
          <h1>Explore</h1>
          <p className="page-description">
            Find a business, choose a service, and check queue availability.
          </p>
        </div>

        {selectedBusiness && (
          <button
            className="secondary-button"
            type="button"
            onClick={
              selectedService
                ? handleBackToServices
                : handleBackToBusinesses
            }
          >
            {selectedService ? 'Back to services' : 'Back to businesses'}
          </button>
        )}
      </div>

      {error && (
        <div className="explore-error" role="alert">
          {error}
        </div>
      )}

      {!selectedBusiness && (
        <>
          <div className="explore-section-header">
            <div>
              <span className="card-label">Available businesses</span>
              <h2>Choose where you want to book</h2>
            </div>
          </div>

          {loadingBusinesses ? (
            <div className="explore-state">
              <strong>Loading businesses...</strong>
            </div>
          ) : businesses.length === 0 ? (
            <div className="explore-state">
              <strong>No businesses available</strong>
              <span>
                There are currently no active businesses available for booking.
              </span>
            </div>
          ) : (
            <div className="explore-grid">
              {businesses.map((business) => (
                <article className="explore-card" key={business.id}>
                  <div className="explore-card-header">
                    <div>
                      <span className="card-label">Business</span>
                      <h2>{business.name}</h2>
                    </div>

                    <span className="explore-status">
                      Available
                    </span>
                  </div>

                  {business.description && (
                    <p className="explore-card-description">
                      {business.description}
                    </p>
                  )}

                  {business.address && (
                    <div className="explore-detail">
                      <span className="card-label">Location</span>
                      <strong>{business.address}</strong>
                    </div>
                  )}

                  <button
                    className="primary-button"
                    type="button"
                    onClick={() => void handleBusinessSelect(business)}
                  >
                    View services
                  </button>
                </article>
              ))}
            </div>
          )}
        </>
      )}

      {selectedBusiness && !selectedService && (
        <>
          <div className="explore-selected-business">
            <span className="card-label">Selected business</span>
            <h2>{selectedBusiness.name}</h2>

            {selectedBusiness.address && (
              <span>{selectedBusiness.address}</span>
            )}
          </div>

          <div className="explore-section-header">
            <div>
              <span className="card-label">Available services</span>
              <h2>Choose a service</h2>
            </div>
          </div>

          {loadingServices ? (
            <div className="explore-state">
              <strong>Loading services...</strong>
            </div>
          ) : services.length === 0 ? (
            <div className="explore-state">
              <strong>No active services available</strong>
              <span>
                This business does not currently have any services available
                for booking.
              </span>
            </div>
          ) : (
            <div className="explore-grid">
              {services.map((service) => (
                <article className="explore-card" key={service.id}>
                  <div className="explore-card-header">
                    <div>
                      <span className="card-label">Service</span>
                      <h2>{service.name}</h2>
                    </div>

                    <span className="explore-status">
                      Active
                    </span>
                  </div>

                  {service.description && (
                    <p className="explore-card-description">
                      {service.description}
                    </p>
                  )}

                  <div className="explore-detail">
                    <span className="card-label">Duration</span>
                    <strong>
                      {service.durationMinutes} minutes
                    </strong>
                  </div>

                  <button
                    className="primary-button"
                    type="button"
                    onClick={() => void handleServiceSelect(service)}
                  >
                    Select service
                  </button>
                </article>
              ))}
            </div>
          )}
        </>
      )}

      {selectedBusiness && selectedService && (
        <>
          <div className="explore-selected-business">
            <span className="card-label">Booking at</span>
            <h2>{selectedBusiness.name}</h2>

            <div className="explore-selection-summary">
              <div>
                <span className="card-label">Service</span>
                <strong>{selectedService.name}</strong>
              </div>

              <div>
                <span className="card-label">Duration</span>
                <strong>
                  {selectedService.durationMinutes} minutes
                </strong>
              </div>
            </div>
          </div>

          <div className="queue-selection-panel">
            <div>
              <span className="card-label">Queue date</span>
              <h2>Choose your appointment date</h2>
            </div>

            <input
              type="date"
              value={selectedDate}
              min={getLocalDate()}
              onChange={(event) => void handleDateChange(event)}
            />
          </div>

          {loadingQueue ? (
            <div className="explore-state">
              <strong>Checking queue availability...</strong>
            </div>
          ) : queue ? (
            <article className="queue-availability-card">
              <div className="explore-card-header">
                <div>
                  <span className="card-label">Queue available</span>
                  <h2>Ready to book</h2>
                </div>

                <span className="queue-status queue-status-active">
                  {queue.status}
                </span>
              </div>

              <div className="queue-availability-details">
                <div>
                  <span className="card-label">Date</span>
                  <strong>{queue.queueDate}</strong>
                </div>

                <div>
                  <span className="card-label">Current token</span>
                  <strong>#{queue.currentToken}</strong>
                </div>
              </div>

              {!bookedAppointment ? (
                <button
                  className="primary-button"
                  type="button"
                  disabled={booking}
                  onClick={() => void handleBookAppointment()}
                >
                  {booking ? 'Booking appointment...' : 'Book appointment'}
                </button>
              ) : (
                <div className="booking-confirmation">
                  <div>
                    <span className="card-label">Appointment confirmed</span>
                    <h2>Your appointment is booked</h2>
                  </div>

                  <div className="booking-confirmation-details">
                    <div>
                      <span className="card-label">Token</span>
                      <strong>#{bookedAppointment.tokenNumber}</strong>
                    </div>

                    <div>
                      <span className="card-label">Date</span>
                      <strong>{bookedAppointment.appointmentDate}</strong>
                    </div>

                    <div>
                      <span className="card-label">Status</span>
                      <strong>{bookedAppointment.status}</strong>
                    </div>
                  </div>
                </div>
              )}
            </article>
          ) : queueNotFound ? (
            <div className="explore-state">
              <strong>No active queue for this date</strong>
              <span>
                Try another date to find an available queue.
              </span>
            </div>
          ) : null}
        </>
      )}
    </section>
  )
}

export default ExplorePage
