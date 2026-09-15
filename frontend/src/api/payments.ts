import { api } from '../lib/api'
import type {
  PaymentDetailDto,
  PaymentMethod,
  PaymentStatus,
  PageResponse,
  PaymentSummaryDto,
  RiskLevel,
  RefundDto,
} from '../types/api'

export const paymentsApi = {
  list: (params: {
    page?: number
    size?: number
    status?: PaymentStatus
    riskLevel?: RiskLevel
    method?: PaymentMethod
  }) => api.get<PageResponse<PaymentSummaryDto>>('/api/payments', { params }),
  detail: (id: string) => api.get<PaymentDetailDto>(`/api/payments/${id}`),
  refund: (id: string, payload: { amount: number; reason?: string }) =>
    api.post<RefundDto>(`/api/payments/${id}/refunds`, payload),
}
