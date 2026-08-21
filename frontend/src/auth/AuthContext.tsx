import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { TOKEN_KEY, authApi } from '../api/client'
import type { MerchantProfile, RegisterPayload } from '../api/types'

interface AuthContextValue {
  profile: MerchantProfile | null
  loading: boolean
  login: (email: string, password: string) => Promise<MerchantProfile>
  register: (payload: RegisterPayload) => Promise<MerchantProfile>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [profile, setProfile] = useState<MerchantProfile | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) {
      setLoading(false)
      return
    }
    authApi
      .me()
      .then(setProfile)
      .catch(() => localStorage.removeItem(TOKEN_KEY))
      .finally(() => setLoading(false))
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const result = await authApi.login(email, password)
    localStorage.setItem(TOKEN_KEY, result.token)
    setProfile(result.profile)
    return result.profile
  }, [])

  const register = useCallback(async (payload: RegisterPayload) => {
    const result = await authApi.register(payload)
    localStorage.setItem(TOKEN_KEY, result.token)
    setProfile(result.profile)
    return result.profile
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    setProfile(null)
  }, [])

  const value = useMemo(
    () => ({ profile, loading, login, register, logout }),
    [profile, loading, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside an AuthProvider')
  }
  return context
}
