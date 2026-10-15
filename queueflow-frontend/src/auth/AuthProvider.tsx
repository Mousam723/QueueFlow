import {
  useCallback,
  useMemo,
  useState,
  type ReactNode,
} from 'react'

import { login as loginApi } from '../api/authApi'
import { ACCESS_TOKEN_KEY } from '../api/client'
import type { AuthUser } from '../types/api'
import { AuthContext } from './AuthContext'

const USER_KEY = 'queueflow_user'

function loadStoredUser(): AuthUser | null {
  const accessToken = localStorage.getItem(ACCESS_TOKEN_KEY)
  const storedUser = localStorage.getItem(USER_KEY)

  if (!accessToken || !storedUser) {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    return null
  }

  try {
    return JSON.parse(storedUser) as AuthUser
  } catch {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    return null
  }
}

function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(loadStoredUser)

  const login = useCallback(async (request: Parameters<typeof loginApi>[0]) => {
    const response = await loginApi(request)

    const authUser: AuthUser = {
      userId: response.userId,
      name: response.name,
      email: response.email,
      role: response.role,
    }

    localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken)
    localStorage.setItem(USER_KEY, JSON.stringify(authUser))

    setUser(authUser)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: user !== null,
      login,
      logout,
    }),
    [user, login, logout],
  )

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

export default AuthProvider
