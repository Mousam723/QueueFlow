import { useEffect, useState } from 'react'

import { ApiError } from '../api/client'
import { getMyBusinesses } from '../api/businessApi'
import {
  assignStaff,
  getAvailableStaff,
  getBusinessStaff,
} from '../api/staffApi'
import { useAuth } from '../auth/useAuth'
import type {
  BusinessResponse,
  StaffResponse,
} from '../types/api'

export default function StaffPage() {
  const { user } = useAuth()

  const [businesses, setBusinesses] = useState<BusinessResponse[]>([])
  const [selectedBusinessId, setSelectedBusinessId] = useState<number | null>(
    null,
  )

  const [staff, setStaff] = useState<StaffResponse[]>([])
  const [availableStaff, setAvailableStaff] = useState<StaffResponse[]>([])

  const [loading, setLoading] = useState(true)
  const [assigningStaffId, setAssigningStaffId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

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
            setStaff([])
            setAvailableStaff([])
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
          setStaff([])
          setAvailableStaff([])
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

    async function loadStaff() {
      setLoading(true)
      setError(null)

      try {
        const [businessStaffResult, availableStaffResult] =
          await Promise.all([
            getBusinessStaff(businessId, 0, 100),
            getAvailableStaff(businessId, 0, 100),
          ])

        if (!cancelled) {
          setStaff(businessStaffResult.content)
          setAvailableStaff(availableStaffResult.content)
        }
      } catch (err) {
        if (!cancelled) {
          if (err instanceof ApiError) {
            setError(err.message)
          } else {
            setError('Unable to load staff.')
          }

          setStaff([])
          setAvailableStaff([])
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    void loadStaff()

    return () => {
      cancelled = true
    }
  }, [selectedBusinessId])

  async function handleAssignStaff(staffMember: StaffResponse) {
    if (selectedBusinessId === null) {
      return
    }

    setAssigningStaffId(staffMember.staffId)
    setError(null)

    try {
      const assignedStaff = await assignStaff(selectedBusinessId, {
        staffId: staffMember.staffId,
      })

      setStaff((currentStaff) => [...currentStaff, assignedStaff])
      setAvailableStaff((currentStaff) =>
        currentStaff.filter(
          (currentMember) =>
            currentMember.staffId !== staffMember.staffId,
        ),
      )
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError('Unable to assign staff member.')
      }
    } finally {
      setAssigningStaffId(null)
    }
  }

  if (loading && businesses.length === 0) {
    return (
      <>
        <div className="page-header">
          <p className="eyebrow">BUSINESS MANAGEMENT</p>
          <h1>Staff</h1>
          <p className="page-description">
            Manage staff members assigned to your business.
          </p>
        </div>

        <div className="empty-state">
          <div className="empty-state-icon">...</div>
          <h2>Loading staff</h2>
          <p>Loading your businesses and staff members.</p>
        </div>
      </>
    )
  }

  return (
    <>
      <div className="page-header">
        <p className="eyebrow">BUSINESS MANAGEMENT</p>
        <h1>Staff</h1>
        <p className="page-description">
          Manage staff members assigned to your business.
        </p>
      </div>

      {error && <div className="queue-error">{error}</div>}

      {businesses.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">+</div>
          <h2>No businesses available</h2>
          <p>
            You need an available business before you can manage staff.
          </p>
        </div>
      ) : (
        <>
          <div className="appointment-toolbar">
            <div>
              <label htmlFor="staff-business">Business</label>
              <select
                id="staff-business"
                className="form-select"
                value={selectedBusinessId ?? ''}
                onChange={(event) =>
                  setSelectedBusinessId(
                    event.target.value
                      ? Number(event.target.value)
                      : null,
                  )
                }
                disabled={loading || assigningStaffId !== null}
              >
                {businesses.map((business) => (
                  <option key={business.id} value={business.id}>
                    {business.name} (#{business.id})
                  </option>
                ))}
              </select>
            </div>

            <span className="toolbar-value">
              {staff.length} staff member{staff.length === 1 ? '' : 's'}
            </span>
          </div>

          <div className="service-management">
            <section className="service-create-card">
              <div className="service-section-header">
                <div>
                  <p className="eyebrow">ASSIGNED STAFF</p>
                  <h2>Current staff</h2>
                </div>
              </div>

              {staff.length === 0 ? (
                <div className="empty-state">
                  <h3>No staff assigned</h3>
                  <p>
                    Assign a staff member from the available staff list.
                  </p>
                </div>
              ) : (
                <div className="service-list">
                  {staff.map((staffMember) => (
                    <article
                      className="service-list-item"
                      key={staffMember.staffId}
                    >
                      <div>
                        <strong>{staffMember.name}</strong>
                        <p>{staffMember.email}</p>
                        {staffMember.phone && (
                          <p>{staffMember.phone}</p>
                        )}
                      </div>

                      <span className="service-status service-status-active">
                        Assigned
                      </span>
                    </article>
                  ))}
                </div>
              )}
            </section>

            <section className="service-create-card">
              <div className="service-section-header">
                <div>
                  <p className="eyebrow">STAFF DIRECTORY</p>
                  <h2>Available staff</h2>
                </div>
              </div>

              {availableStaff.filter(
                (availableMember) =>
                  !staff.some(
                    (assignedMember) =>
                      assignedMember.staffId === availableMember.staffId,
                  ),
              ).length === 0 ? (
                <div className="empty-state">
                  <h3>No available staff</h3>
                  <p>
                    There are currently no unassigned staff members.
                  </p>
                </div>
              ) : (
                <div className="service-list">
                  {availableStaff
                    .filter(
                      (availableMember) =>
                        !staff.some(
                          (assignedMember) =>
                            assignedMember.staffId ===
                            availableMember.staffId,
                        ),
                    )
                    .map((staffMember) => (
                      <article
                        className="service-list-item"
                        key={staffMember.staffId}
                      >
                        <div>
                          <strong>{staffMember.name}</strong>
                          <p>{staffMember.email}</p>
                          {staffMember.phone && (
                            <p>{staffMember.phone}</p>
                          )}
                        </div>

                        <button
                          type="button"
                          className="queue-action-primary"
                          onClick={() =>
                            void handleAssignStaff(staffMember)
                          }
                          disabled={assigningStaffId !== null}
                        >
                          {assigningStaffId === staffMember.staffId
                            ? 'Assigning...'
                            : 'Assign'}
                        </button>
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
