import axios from 'axios'
import type {
  AuthResponse,
  CreateOrderPayload,
  DashboardSummary,
  FraudAnalysis,
  InitiatePaymentPayload,
  MerchantFeedback,
  MerchantProfile,
  Order,
  PageResponse,
  Payment,
  PaymentDetail,
  PublicOrder,
  PublicPayment,
  Refund,
  RegisterPayload,
} from './types'

export const TOKEN_KEY = 'payflux.token'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/** Turns an axios failure into the backend's message when it has one. */
export function apiErrorMessage(error: unknown, fallback = 'Something went wrong'): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string; error?: string } | undefined
    return data?.message ?? data?.error ?? error.message ?? fallback
  }
  return fallback
}

export function newIdempotencyKey(): string {
  return `pf_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 10)}`
}

export const authApi = {
  async register(payload: RegisterPayload) {
    const { data } = await api.post<AuthResponse>('/api/auth/register', payload)
    return data
  },
  async login(email: string, password: string) {
    const { data } = await api.post<AuthResponse>('/api/auth/login', { email, password })
    return data
  },
  async me() {
    const { data } = await api.get<MerchantProfile>('/api/auth/me')
    return data
  },
}

export const orderApi = {
  async create(payload: CreateOrderPayload, idempotencyKey: string) {
    const { data } = await api.post<Order>('/api/orders', payload, {
      headers: { 'X-Idempotency-Key': idempotencyKey },
    })
    return data
  },
  async list(params: { status?: string; page?: number; size?: number } = {}) {
    const { data } = await api.get<PageResponse<Order>>('/api/orders', { params })
    return data
  },
  async get(orderId: string) {
    const { data } = await api.get<{ order: Order; payments: Payment[] }>(`/api/orders/${orderId}`)
    return data
  },
}

export const paymentApi = {
  async list(params: { status?: string; page?: number; size?: number } = {}) {
    const { data } = await api.get<PageResponse<Payment>>('/api/payments', { params })
    return data
  },
  async detail(paymentId: string) {
    const { data } = await api.get<PaymentDetail>(`/api/payments/${paymentId}`)
    return data
  },
  async refund(paymentId: string, amount: number | null, reason: string) {
    const { data } = await api.post<Refund>(`/api/payments/${paymentId}/refunds`, { amount, reason })
    return data
  },
}

export const fraudApi = {
  async alerts(params: { page?: number; size?: number } = {}) {
    const { data } = await api.get<PageResponse<FraudAnalysis>>('/api/fraud-alerts', { params })
    return data
  },
  async feedback(analysisId: string, feedback: MerchantFeedback, note?: string) {
    const { data } = await api.post<FraudAnalysis>(`/api/fraud-alerts/${analysisId}/feedback`, {
      feedback,
      note,
    })
    return data
  },
}

export const refundApi = {
  async list(params: { page?: number; size?: number } = {}) {
    const { data } = await api.get<PageResponse<Refund>>('/api/refunds', { params })
    return data
  },
}

export const dashboardApi = {
  async summary() {
    const { data } = await api.get<DashboardSummary>('/api/dashboard/summary')
    return data
  },
}

export const checkoutApi = {
  async order(orderId: string) {
    const { data } = await api.get<PublicOrder>(`/api/public/orders/${orderId}`)
    return data
  },
  async pay(payload: InitiatePaymentPayload, idempotencyKey: string) {
    const { data } = await api.post<PublicPayment>('/api/public/payments', payload, {
      headers: { 'X-Idempotency-Key': idempotencyKey },
    })
    return data
  },
  async verify(paymentId: string, otp: string) {
    const { data } = await api.post<PublicPayment>(`/api/public/payments/${paymentId}/verify`, { otp })
    return data
  },
}
