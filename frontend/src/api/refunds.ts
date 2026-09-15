import { api } from '../lib/api'
import type { PageResponse, RefundDto, RefundStatus } from '../types/api'

export const refundsApi = {
  list: (params: { page?: number; size?: number; status?: RefundStatus }) =>
    api.get<PageResponse<RefundDto>>('/api/refunds', { params }),
  detail: (id: string) => api.get<RefundDto>(`/api/refunds/${id}`),
}
