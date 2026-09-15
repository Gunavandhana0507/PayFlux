import { api } from '../lib/api'
import type {
  FraudAnalysisDto,
  MerchantFeedback,
  PageResponse,
  PaymentSummaryDto,
} from '../types/api'

export const fraudApi = {
  list: (params: { page?: number; size?: number }) =>
    api.get<PageResponse<PaymentSummaryDto>>('/api/fraud-alerts', { params }),
  feedback: (id: string, payload: { feedback: MerchantFeedback; note?: string }) =>
    api.post<FraudAnalysisDto>(`/api/fraud-alerts/${id}/feedback`, payload),
}
