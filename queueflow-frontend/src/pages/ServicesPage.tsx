import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '../api/client'
import { getMyBusinesses } from '../api/businessApi'
import {
  createBusinessService,
  deactivateBusinessService,
  getBusinessServices,
  reactivateBusinessService,
} from '../api/serviceApi'
import { useAuth } from '../auth/useAuth'
import type {
  BusinessResponse,
  ServiceResponse,
} from '../types/api'

export default function ServicesPage() {
  const { user } = useAuth()

  const [businesses, setBusinesses] = useState<BusinessResponse[]>([])
  const [selectedBusinessId, setSelectedBusinessId] = useState<number | null>(
    null,
  )
  const [services, setServices] = useState<ServiceResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [serviceActionId, setServiceActionId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [durationMinutes, setDurationMinutes] = useState('30')

  useEffect(() => {
    let cancelled = false

    async function loadBusinesses() {
      if (!user) {
        return
      }

      setLoading(true)
      setError(null)

      try {
        const result = await getMyBusinesses()

        if (!cancelled) {
          setBusinesses(result.content)

          if (result.content.length > 0) {
            setSelectedBusinessId(result.content[0].id)
          } else {
            setSelectedBusinessId(null)
            setServices([])
          }
        }
      } catch (err) {
        if (!cancelled) {
          if (err instanceof ApiError) {
            setError(err.message)
          } else {
            setError('Unable to load businesses.')
          }

          setBusinesses([])
          setSelectedBusinessId(null)
          setServices([])
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    void loadBusinesses()

    return () => {
      cancelled = true
    }
  }, [user])

  useEffect(() => {
    if (selectedBusinessId === null) {
      return
    }

    const businessId = selectedBusinessId
    let cancelled = false

    async function loadServices() {
      setLoading(true)
      setError(null)

      try {
        const result = await getBusinessServices(businessId)

        if (!cancelled) {
          setServices(result.content)
        }
      } catch (err) {
        if (!cancelled) {
          if (err instanceof ApiError) {
            setError(err.message)
          } else {
            setError('Unable to load services.')
          }

          setServices([])
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    void loadServices()

    return () => {
      cancelled = true
    }
  }, [selectedBusinessId])

  async function handleCreateService(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (selectedBusinessId === null) {
      setError('Select a business before creating a service.')
      return
    }

    const parsedDuration = Number(durationMinutes)

    if (!Number.isInteger(parsedDuration) || parsedDuration < 1) {
      setError('Duration must be a whole number greater than 0.')
      return
    }

    setSubmitting(true)
    setError(null)

    try {
      const createdService = await createBusinessService(selectedBusinessId, {
        name: name.trim(),
        description: description.trim() || undefined,
        durationMinutes: parsedDuration,
      })

      setServices((currentServices) => [
        ...currentServices,
        createdService,
      ])

      setName('')
      setDescription('')
      setDurationMinutes('30')
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError('Unable to create service.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  if (loading && businesses.length === 0) {
    return (
      <>
        <div className="page-header">
          <p className="eyebrow">BUSINESS MANAGEMENT</p>
          <h1>Services</h1>
          <p className="page-description">
            Configure the services offered by your business.
          </p>
        </div>
        <div className="empty-state">
          <div className="empty-state-icon">...</div>
          <h2>Loading services</h2>
          <p>Loading your businesses and available services.</p>
        </div>
      </>
    )
  }

  async function handleServiceStatusChange(service: ServiceResponse) {
    if (selectedBusinessId === null) {
      return
    }

    const businessId = selectedBusinessId
    setServiceActionId(service.id)
    setError(null)

    try {
      const updatedService =
        service.status === 'ACTIVE'
          ? await deactivateBusinessService(businessId, service.id)
          : await reactivateBusinessService(businessId, service.id)

      setServices((currentServices) =>
        currentServices.map((currentService) =>
          currentService.id === updatedService.id
            ? updatedService
            : currentService,
        ),
      )
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError(
          service.status === 'ACTIVE'
            ? 'Unable to deactivate service.'
            : 'Unable to reactivate service.',
        )
      }
    } finally {
      setServiceActionId(null)
    }
  }

  return (
    <>
      <div className="page-header">
        <p className="eyebrow">BUSINESS MANAGEMENT</p>
        <h1>Services</h1>
        <p className="page-description">
          Configure the services offered by your business.
        </p>
      </div>

      {error && <div className="queue-error">{error}</div>}

      {businesses.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">+</div>
          <h2>No businesses available</h2>
          <p>
            You need an available business before you can create or manage
            services.
          </p>
        </div>
      ) : (
        <>
          <div className="appointment-toolbar">
            <div>
              <label htmlFor="service-business">Business</label>
              <select
                id="service-business"
                className="form-select"
                value={selectedBusinessId ?? ''}
                onChange={(event) =>
                  setSelectedBusinessId(
                    event.target.value
                      ? Number(event.target.value)
                      : null,
                  )
                }
                disabled={loading || submitting}
              >
                {businesses.map((business) => (
                  <option key={business.id} value={business.id}>
                    {business.name} (#{business.id})
                  </option>
                ))}
              </select>
            </div>

            <span className="toolbar-value">
              {services.length} service{services.length === 1 ? '' : 's'}
            </span>
          </div>

          <div className="service-management">
            <section className="service-create-card">
              <div className="service-section-header">
                <div>
                  <p className="eyebrow">NEW SERVICE</p>
                  <h2>Create service</h2>
                </div>
              </div>

              <form
                className="service-form"
                onSubmit={handleCreateService}
              >
                <label>
                  Service name
                  <input
                    type="text"
                    value={name}
                    onChange={(event) => setName(event.target.value)}
                    placeholder="e.g. General Consultation"
                    maxLength={150}
                    required
                    disabled={submitting}
                  />
                </label>

                <label>
                  Description <span className="optional">(optional)</span>
                  <textarea
                    value={description}
                    onChange={(event) =>
                      setDescription(event.target.value)
                    }
                    placeholder="Describe what this service includes."
                    maxLength={500}
                    rows={4}
                    disabled={submitting}
                  />
                </label>

                <label>
                  Duration (minutes)
                  <input
                    type="number"
                    min="1"
                    step="1"
                    value={durationMinutes}
                    onChange={(event) =>
                      setDurationMinutes(event.target.value)
                    }
                    required
                    disabled={submitting}
                  />
                </label>

                <button
                  type="submit"
                  className="primary-button"
                  disabled={
                    submitting ||
                    selectedBusinessId === null ||
                    name.trim().length === 0
                  }
                >
                  {submitting ? 'Creating...' : 'Create service'}
                </button>
              </form>
            </section>

            <section>
              <div className="service-section-header">
                <div>
                  <p className="eyebrow">AVAILABLE SERVICES</p>
                  <h2>Service catalog</h2>
                </div>
              </div>

              {loading ? (
                <div className="empty-state">
                  <h2>Loading services</h2>
                  <p>Fetching the latest service catalog.</p>
                </div>
              ) : services.length === 0 ? (
                <div className="empty-state">
                  <div className="empty-state-icon">+</div>
                  <h2>No services yet</h2>
                  <p>
                    Create the first service for this business using the
                    form above.
                  </p>
                </div>
              ) : (
                <div className="service-list">
                  {services.map((service) => (
                    <article className="service-card" key={service.id}>
                      <div className="service-card-header">
                        <div>
                          <h2>{service.name}</h2>
                          <span className="service-id">
                            Service #{service.id}
                          </span>
                        </div>

                        <span
                          className={`service-status service-status-${service.status.toLowerCase()}`}
                        >
                          {service.status}
                        </span>
                      </div>

                      <div className="service-details">
                        <div>
                          <span className="card-label">Duration</span>
                          <strong>
                            {service.durationMinutes} minutes
                          </strong>
                        </div>

                        <div>
                          <span className="card-label">Description</span>
                          <strong>
                            {service.description || 'No description'}
                          </strong>
                        </div>
                      </div>
                      <div className="service-card-actions">
                        <button
                          type="button"
                          className="secondary-button"
                          onClick={() => void handleServiceStatusChange(service)}
                          disabled={
                            serviceActionId !== null ||
                            submitting ||
                            selectedBusinessId === null
                          }
                        >
                          {serviceActionId === service.id
                            ? service.status === 'ACTIVE'
                              ? 'Deactivating...'
                              : 'Reactivating...'
                            : service.status === 'ACTIVE'
                              ? 'Deactivate'
                              : 'Reactivate'}
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </section>
          </div>
        </>
      )}
    </>
  )
}
