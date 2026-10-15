import { useEffect, useState } from 'react'
import { ApiError } from '../api/client'

import { getMyBusinesses } from '../api/businessApi'

import { getBusinessServices } from '../api/serviceApi'

import { useAuth } from '../auth/useAuth'
import {
  advanceQueue,
  closeQueue,
  createQueue,
  getBusinessQueues,
  pauseQueue,
  resumeQueue,
} from '../api/queueApi'
import type {
  BusinessResponse,
  QueueResponse,
  ServiceResponse,
} from '../types/api'

function formatQueueDate(date: string) {
  const parsedDate = new Date(`${date}T00:00:00`)

  return parsedDate.toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function getToday() {
  const today = new Date()

  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function QueuesPage() {
  const { user } = useAuth()

  const [selectedDate, setSelectedDate] = useState(getToday)
  const [businesses, setBusinesses] = useState<BusinessResponse[]>([])
  const [selectedBusinessId, setSelectedBusinessId] = useState<number | null>(
    null,
  )
  const [services, setServices] = useState<ServiceResponse[]>([])
  const [queues, setQueues] = useState<QueueResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState<number | null>(null)
  const [creatingQueue, setCreatingQueue] = useState(false)
  const [createQueueServiceId, setCreateQueueServiceId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) {
      return
    }

    let cancelled = false

    async function loadBusinesses() {
      setLoading(true)
      setError(null)

      try {
        const result = await getMyBusinesses()

        if (cancelled) {
          return
        }

        setBusinesses(result.content)

        const firstBusinessId = result.content[0]?.id ?? null
        setSelectedBusinessId(firstBusinessId)
      } catch (err) {
        if (!cancelled) {
          if (err instanceof ApiError) {
            setError(err.message)
          } else {
            setError('Unable to load businesses.')
          }

          setBusinesses([])
          setSelectedBusinessId(null)
          setQueues([])
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

    async function loadBusinessData() {
      setLoading(true)
      setError(null)

      try {
        const [queueResult, serviceResult] = await Promise.all([
          getBusinessQueues(businessId, selectedDate),
          getBusinessServices(businessId),
        ])

        if (!cancelled) {
          setQueues(queueResult.content)
          setServices(serviceResult.content)
        }
      } catch (err) {
        if (!cancelled) {
          if (err instanceof ApiError) {
            setError(err.message)
          } else {
            setError('Unable to load queue data.')
          }

          setQueues([])
          setServices([])
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    void loadBusinessData()

    return () => {
      cancelled = true
    }
  }, [selectedBusinessId, selectedDate])

  async function handleCreateQueue() {
    if (selectedBusinessId === null) {
      setError('Select a business before creating a queue.')
      return
    }

    if (createQueueServiceId === null) {
      setError('Select a service before creating a queue.')
      return
    }

    setCreatingQueue(true)
    setError(null)

    try {
      const createdQueue = await createQueue(selectedBusinessId, {
        serviceId: createQueueServiceId,
        queueDate: selectedDate,
      })

      setQueues((currentQueues) => [
        ...currentQueues,
        createdQueue,
      ])

      setCreateQueueServiceId(null)
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError('Unable to create queue.')
      }
    } finally {
      setCreatingQueue(false)
    }
  }

  async function handleQueueAction(
    queueId: number,
    action: 'advance' | 'pause' | 'resume' | 'close',
  ) {
    setActionLoading(queueId)
    setError(null)

    try {
      let updatedQueue: QueueResponse

      switch (action) {
        case 'advance':
          updatedQueue = await advanceQueue(queueId)
          break

        case 'pause':
          updatedQueue = await pauseQueue(queueId)
          break

        case 'resume':
          updatedQueue = await resumeQueue(queueId)
          break

        case 'close':
          updatedQueue = await closeQueue(queueId)
          break
      }

      setQueues((currentQueues) =>
        currentQueues.map((queue) =>
          queue.id === queueId ? updatedQueue : queue,
        ),
      )
    } catch (err) {
      if (err instanceof ApiError) {
        if (action === 'advance' && err.status === 409) {
          setError(
            'No waiting appointments. There are currently no customers waiting in this queue.',
          )
        } else {
          setError(err.message)
        }
      } else {
        setError('Unable to update queue.')
      }
    } finally {
      setActionLoading(null)
    }
  }

  return (
    <section>
      <div className="page-header">
        <p className="eyebrow">QUEUE MANAGEMENT</p>

        <h1>Queues</h1>

        <p className="page-description">
          Monitor today's queues and their current serving status.
        </p>
      </div>

      <div className="queue-toolbar">
        <div>
          <label htmlFor="queue-date">Queue date</label>

          <input
            id="queue-date"
            type="date"
            value={selectedDate}
            onChange={(event) => setSelectedDate(event.target.value)}
          />
        </div>

        <div>
          <label htmlFor="queue-business">Business</label>
          <select
            id="queue-business"
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
        </div>

        <div className="queue-create-form">
          <label htmlFor="queue-service">Service</label>
          <select
            id="queue-service"
            className="form-select"
            value={createQueueServiceId ?? ''}
            onChange={(event) =>
              setCreateQueueServiceId(
                event.target.value ? Number(event.target.value) : null,
              )
            }
            disabled={
              creatingQueue ||
              loading ||
              selectedBusinessId === null
            }
          >
            <option value="">Select a service</option>
            {services
              .filter((service) => service.status === 'ACTIVE')
              .filter(
                (service) =>
                  !queues.some(
                    (queue) =>
                      queue.serviceId === service.id &&
                      queue.queueDate === selectedDate,
                  ),
              )
              .map((service) => (
                <option key={service.id} value={service.id}>
                  {service.name} (#{service.id})
                </option>
              ))}
          </select>

          <button
            type="button"
            className="queue-action-primary"
            onClick={() => void handleCreateQueue()}
            disabled={
              creatingQueue ||
              loading ||
              selectedBusinessId === null ||
              createQueueServiceId === null
            }
          >
            {creatingQueue ? 'Creating Queue...' : 'Create Queue'}
          </button>
        </div>
      </div>

      {error && (
        <div className="queue-error" role="alert">
          {error}
        </div>
      )}

      {loading && (
        <div className="empty-state">
          <h2>Loading queues...</h2>
          <p>Fetching queue data from the backend.</p>
        </div>
      )}

      {!loading && !error && queues.length === 0 && (
        <div className="empty-state">
          <div className="empty-state-icon">Q</div>

          <h2>No queues found</h2>

          <p>
            There are no queues for this business on {selectedDate}.
          </p>
        </div>
      )}

      {!loading && queues.length > 0 && (
        <div className="queue-list">
          {queues.map((queue) => {
            const isUpdating = actionLoading === queue.id

            return (
              <article className="queue-card" key={queue.id}>
                <div className="queue-card-header">
                  <div>
                    <span className="card-label">
                      Queue #{queue.id}
                    </span>
                    <h2>
                      {services.find(
                        (service) => service.id === queue.serviceId,
                      )?.name ?? `Service #${queue.serviceId}`}
                    </h2>
                    <span className="queue-service-id">
                      Service #{queue.serviceId}
                    </span>
                  </div>

                  <span
                    className={`queue-status queue-status-${queue.status.toLowerCase()}`}
                  >
                    {queue.status}
                  </span>
                </div>

                <div className="queue-card-body">
                  <div>
                    <span className="card-label">Current token</span>

                    <strong className="queue-token">
                      {queue.currentToken}
                    </strong>
                  </div>

                  <div className="queue-date-detail">
                    <span className="card-label">Queue date</span>
                    <strong className="queue-date-value">
                      {formatQueueDate(queue.queueDate)}
                    </strong>
                  </div>
                </div>

                <div className="queue-actions">
                  {queue.status === 'ACTIVE' && (
                    <>
                      <button
                        type="button"
                        className="queue-action-primary"
                        disabled={isUpdating}
                        onClick={() =>
                          void handleQueueAction(queue.id, 'advance')
                        }
                      >
                        {isUpdating ? 'Updating...' : 'Advance Queue'}
                      </button>

                      <button
                        type="button"
                        className="queue-action-secondary"
                        disabled={isUpdating}
                        onClick={() =>
                          void handleQueueAction(queue.id, 'pause')
                        }
                      >
                        Pause
                      </button>

                      <button
                        type="button"
                        className="queue-action-danger"
                        disabled={isUpdating}
                        onClick={() =>
                          void handleQueueAction(queue.id, 'close')
                        }
                      >
                        Close
                      </button>
                    </>
                  )}

                  {queue.status === 'PAUSED' && (
                    <>
                      <button
                        type="button"
                        className="queue-action-primary"
                        disabled={isUpdating}
                        onClick={() =>
                          void handleQueueAction(queue.id, 'resume')
                        }
                      >
                        {isUpdating ? 'Updating...' : 'Resume Queue'}
                      </button>

                      <button
                        type="button"
                        className="queue-action-danger"
                        disabled={isUpdating}
                        onClick={() =>
                          void handleQueueAction(queue.id, 'close')
                        }
                      >
                        Close
                      </button>
                    </>
                  )}

                  {queue.status === 'CLOSED' && (
                    <span className="queue-closed-message">
                      This queue is closed.
                    </span>
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

export default QueuesPage