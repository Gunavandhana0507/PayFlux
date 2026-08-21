import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '../auth/AuthContext'
import { LoadingSpinner } from './ui/Feedback'

export function ProtectedRoute({
  children,
  role,
}: {
  children: ReactNode
  role?: 'MERCHANT' | 'ADMIN'
}) {
  const { profile, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <LoadingSpinner label="Loading your workspace…" />
  }
  if (!profile) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  if (role && profile.role !== role) {
    return <Navigate to="/dashboard" replace />
  }
  return <>{children}</>
}
