import { Navigate, Outlet, useLocation } from 'react-router-dom'
import type { UserRole } from '../types/api'
import { useAuth } from './useAuth'

interface RoleRouteProps {
  allowedRoles: UserRole[]
}

function RoleRoute({ allowedRoles }: RoleRouteProps) {
  const { user } = useAuth()
  const location = useLocation()

  if (!user || !allowedRoles.includes(user.role)) {
    return <Navigate to="/" replace state={{ from: location.pathname }} />
  }

  return <Outlet />
}

export default RoleRoute
