import { api } from '../lib/api'
import type { CreateOrderRequest, OrderDetailDto, OrderDto, OrderStatus, PageResponse } from '../types/api'

export const ordersApi = {
  list: (params: { page?: number; size?: number; status?: OrderStatus; from?: string; to?: string }) =>
    api.get<PageResponse<OrderDto>>('/api/orders', { params }),
  detail: (id: string) => api.get<OrderDetailDto>(`/api/orders/${id}`),
  create: (payload: CreateOrderRequest, key: string) =>
    api.post<OrderDto>('/api/orders', payload, { headers: { 'X-Idempotency-Key': key } }),
}
