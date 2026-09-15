import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { LoadingSpinner } from './ui/LoadingSpinner'

export function RequireAuth() {
  const { token, ready } = useAuth()
  const location = useLocation()
  if (!ready) return <LoadingSpinner fullPage />
  if (!token) return <Navigate to={`/login?from=${encodeURIComponent(location.pathname)}`} replace />
  return <Outlet />
}
