import { useEffect, useState } from 'react'
import { ApiError } from '../api/client'
import { getQueue } from '../api/queueApi'
import { getBusinessServices } from '../api/serviceApi'
import type { QueueResponse, ServiceResponse } from '../types/api'

interface QueueStatusCardProps {
  queueId: number
}

function QueueStatusCard({ queueId }: QueueStatusCardProps) {
  const [queue, setQueue] = useState<QueueResponse | null>(null)
  const [service, setService] = useState<ServiceResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function loadQueueData() {
      setLoading(true)
      setError(null)

      try {
        const queueResult = await getQueue(queueId)
        const services = await getBusinessServices(queueResult.businessId)

        const serviceResult =
          services.content.find((item) => item.id === queueResult.serviceId) ?? null

        if (!cancelled) {
          setQueue(queueResult)
          setService(serviceResult)
        }
      } catch (err) {
        if (!cancelled) {
          if (err instanceof ApiError) {
            setError(err.message)
          } else {
            setError('Unable to load queue data.')
          }
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    void loadQueueData()

    return () => {
      cancelled = true
    }
  }, [queueId])

  if (loading) {
    return (
      <article className="dashboard-card">
        <span className="card-label">Queue</span>
        <strong className="card-value">...</strong>
        <span className="card-meta">Loading queue data</span>
      </article>
    )
  }

  if (error) {
    return (
      <article className="dashboard-card">
        <span className="card-label">Queue</span>
        <strong className="card-value">—</strong>
        <span className="card-meta">{error}</span>
      </article>
    )
  }

  if (!queue) {
    return null
  }

  return (
    <article className="dashboard-card">
      <span className="card-label">Queue #{queue.id}</span>

      <strong className="card-value">
        {queue.currentToken}
      </strong>

      <span className="card-meta">
        {queue.status} · {service?.name ?? `Service #${queue.serviceId}`}
      </span>
    </article>
  )
}

export default QueueStatusCard