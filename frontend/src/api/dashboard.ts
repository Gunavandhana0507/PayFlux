import { api } from '../lib/api'
import type { DashboardSummary } from '../types/api'

export const dashboardApi = {
  summary: () => api.get<DashboardSummary>('/api/dashboard/summary'),
}
