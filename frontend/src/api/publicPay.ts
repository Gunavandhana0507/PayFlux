import { api } from '../lib/api'
import type { PaymentRequest, PublicOrderDto, PublicPaymentDto } from '../types/api'

export const publicPayApi = {
  order: (id: string) => api.get<PublicOrderDto>(`/api/public/orders/${id}`),
  pay: (id: string, payload: PaymentRequest, key: string) =>
    api.post<PublicPaymentDto>(`/api/public/orders/${id}/payments`, payload, {
      headers: { 'X-Idempotency-Key': key },
    }),
  verify: (id: string, otp: string) =>
    api.post<PublicPaymentDto>(`/api/public/payments/${id}/verify`, { otp }),
}
