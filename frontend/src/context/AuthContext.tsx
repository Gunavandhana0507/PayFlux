import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { authApi, type LoginPayload, type RegisterPayload } from '../api/auth'
import type { MerchantDto } from '../types/api'

interface AuthContextValue {
  merchant: MerchantDto | null
  token: string | null
  ready: boolean
  login: (payload: LoginPayload) => Promise<void>
  register: (payload: RegisterPayload) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState(() => localStorage.getItem('payflux_token'))
  const [merchant, setMerchant] = useState<MerchantDto | null>(() => {
    const value = localStorage.getItem('payflux_merchant')
    return value ? (JSON.parse(value) as MerchantDto) : null
  })
  const [ready, setReady] = useState(false)

  useEffect(() => {
    if (!token) {
      setReady(true)
      return
    }
    authApi
      .me()
      .then(({ data }) => {
        setMerchant(data)
        localStorage.setItem('payflux_merchant', JSON.stringify(data))
      })
      .catch(() => {
        localStorage.removeItem('payflux_token')
        localStorage.removeItem('payflux_merchant')
        setToken(null)
        setMerchant(null)
      })
      .finally(() => setReady(true))
  }, [token])

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      merchant,
      ready,
      login: async (payload) => {
        const { data } = await authApi.login(payload)
        localStorage.setItem('payflux_token', data.token)
        localStorage.setItem('payflux_merchant', JSON.stringify(data.merchant))
        setToken(data.token)
        setMerchant(data.merchant)
      },
      register: async (payload) => {
        const { data } = await authApi.register(payload)
        localStorage.setItem('payflux_token', data.token)
        localStorage.setItem('payflux_merchant', JSON.stringify(data.merchant))
        setToken(data.token)
        setMerchant(data.merchant)
      },
      logout: () => {
        localStorage.removeItem('payflux_token')
        localStorage.removeItem('payflux_merchant')
        setToken(null)
        setMerchant(null)
      },
    }),
    [merchant, ready, token],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const value = useContext(AuthContext)
  if (!value) throw new Error('AuthProvider is missing')
  return value
}
