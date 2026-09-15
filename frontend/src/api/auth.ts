import { api } from '../lib/api'
import type { AuthResponse, MerchantDto } from '../types/api'

export interface RegisterPayload {
  businessName: string
  businessType: string
  gstId: string
  email: string
  password: string
}

export interface LoginPayload {
  email: string
  password: string
}

export const authApi = {
  register: (payload: RegisterPayload) => api.post<AuthResponse>('/api/auth/register', payload),
  login: (payload: LoginPayload) => api.post<AuthResponse>('/api/auth/login', payload),
  me: () => api.get<MerchantDto>('/api/auth/me'),
}
