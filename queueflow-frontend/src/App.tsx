import { NavLink, Navigate, Route, Routes, useNavigate } from 'react-router-dom'

import './App.css'

import { useAuth } from './auth/useAuth'
import ProtectedRoute from './auth/ProtectedRoute'
import RoleRoute from './auth/RoleRoute'
import AppointmentsPage from './pages/AppointmentsPage'
import DashboardPage from './pages/DashboardPage'
import ExplorePage from './pages/ExplorePage'
import LoginPage from './pages/LoginPage'
import QueuesPage from './pages/QueuesPage'
import RegisterPage from './pages/RegisterPage'
import ServicesPage from './pages/ServicesPage'
import StaffPage from './pages/StaffPage'

function AuthenticatedLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="app">
      <header className="app-header">
        <div className="brand">
          <span className="brand-mark">Q</span>
          <span className="brand-name">QueueFlow</span>
        </div>

        <nav className="app-nav" aria-label="Main navigation">
          <NavLink to="/" end>
            Dashboard
          </NavLink>
          {user?.role === 'CUSTOMER' && (
            <NavLink to="/explore">
              Explore
            </NavLink>
          )}

          {user?.role !== 'CUSTOMER' && (
            <NavLink to="/queues">
              Queues
            </NavLink>
          )}

          <NavLink to="/appointments">
            Appointments
          </NavLink>

          {(user?.role === 'BUSINESS_OWNER' || user?.role === 'ADMIN') && (
            <>
              <NavLink to="/services">
                Services
              </NavLink>
              <NavLink to="/staff">
                Staff
              </NavLink>
            </>
          )}
        </nav>

        <div className="header-user">
          <span className="user-avatar">
            {user?.name.charAt(0).toUpperCase() ?? 'U'}
          </span>

          <span className="user-label">
            {user?.name ?? 'User'}
          </span>

          <button
            className="logout-button"
            type="button"
            onClick={handleLogout}
          >
            Logout
          </button>
        </div>
      </header>

      <main className="app-main">
        <Routes>
          <Route path="/" element={<DashboardPage />} />
          <Route
            element={<RoleRoute allowedRoles={['CUSTOMER']} />}
          >
            <Route path="/explore" element={<ExplorePage />} />
          </Route>

          <Route element={<RoleRoute allowedRoles={['BUSINESS_OWNER', 'STAFF', 'ADMIN']} />}>
            <Route path="/queues" element={<QueuesPage />} />
          </Route>

          <Route path="/appointments" element={<AppointmentsPage />} />
          <Route
            element={
              <RoleRoute allowedRoles={['BUSINESS_OWNER', 'ADMIN']} />
            }
          >
            <Route path="/services" element={<ServicesPage />} />
            <Route path="/staff" element={<StaffPage />} />
          </Route>
        </Routes>
      </main>
    </div>
  )
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<ProtectedRoute />}>
        <Route path="/*" element={<AuthenticatedLayout />} />
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}

export default App
