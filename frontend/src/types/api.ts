export type OrderStatus = 'CREATED' | 'PAID' | 'EXPIRED'
export type PaymentStatus =
  | 'CREATED'
  | 'INITIATED'
  | 'FRAUD_CHECK'
  | 'AUTHORIZED'
  | 'VERIFICATION_REQUIRED'
  | 'REJECTED'
  | 'PROCESSING'
  | 'CAPTURED'
  | 'FAILED'
  | 'PARTIALLY_REFUNDED'
  | 'REFUNDED'
export type PaymentMethod = 'CARD' | 'UPI' | 'NETBANKING' | 'WALLET'
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'
export type RefundStatus = 'PENDING' | 'PROCESSING' | 'PROCESSED' | 'FAILED'
export type MerchantFeedback = 'CONFIRMED_FRAUD' | 'FALSE_POSITIVE'
export type Prediction = 'LEGITIMATE' | 'SUSPICIOUS' | 'LIKELY_FRAUD'
export type AnalysisStatus = 'COMPLETED' | 'FAILED'
export type TransitionActor = 'SYSTEM' | 'CUSTOMER' | 'MERCHANT'
export type ProcessorOutcome = 'SUCCESS' | 'FAILURE' | 'TIMEOUT'

export interface MerchantDto {
  id: number
  businessName: string
  businessType: string
  gstId: string
  email: string
  status: string
  createdAt: string
}

export interface AuthResponse {
  token: string
  merchant: MerchantDto
}

export interface ApiError {
  code: string
  message: string
  fieldErrors?: Record<string, string>
}

export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  total: number
}

export interface OrderDto {
  id: string
  amount: number
  currency: string
  notes: string | null
  customerEmail: string | null
  status: OrderStatus
  expiresAt: string
  createdAt: string
  paymentUrl: string
  latestPaymentId: string | null
  latestPaymentStatus: PaymentStatus | null
}

export interface OrderDetailDto {
  order: OrderDto
  payments: PaymentSummaryDto[]
}

export interface PaymentSummaryDto {
  id: string
  orderId: string
  amount: number
  currency: string
  method: PaymentMethod
  methodSummary: string
  status: PaymentStatus
  riskLevel: RiskLevel | null
  riskScore: number | null
  customerEmail: string
  refundedAmount: number
  createdAt: string
}

export interface PublicPaymentDto {
  id: string
  orderId: string
  status: PaymentStatus
  method: PaymentMethod
  methodSummary: string
  amount: number
  currency: string
  failureReason: string | null
  verificationRequired: boolean
  createdAt: string
}

export interface PublicOrderDto {
  id: string
  merchantName: string
  amount: number
  currency: string
  notes: string | null
  status: OrderStatus
  expiresAt: string
  secondsRemaining: number
  payableNow: boolean
  latestPayment: PublicPaymentDto | null
}

export interface FraudFactor {
  code: string
  description: string
  weight: number
}

export interface FraudFeatures {
  amount: number
  customerAvgAmount: number | null
  attemptsLast10Min: number
  failedAttemptsLast10Min: number
  newDevice: boolean
  deviceRiskScore: number
  locationRiskScore: number
  unusualTransaction: boolean
}

export interface FraudAnalysisDto {
  id: string
  paymentId: string
  riskScore: number
  riskLevel: RiskLevel
  prediction: Prediction
  modelVersion: string
  analysisStatus: AnalysisStatus
  factors: FraudFactor[]
  features: FraudFeatures | null
  merchantFeedback: MerchantFeedback | null
  feedbackAt: string | null
  feedbackNote: string | null
  createdAt: string
}

export interface RefundDto {
  id: string
  paymentId: string
  orderId: string
  amount: number
  currency: string
  status: RefundStatus
  reason: string | null
  failureReason: string | null
  createdAt: string
  processedAt: string | null
}

export interface TransitionDto {
  fromStatus: PaymentStatus
  toStatus: PaymentStatus
  actor: TransitionActor
  reason: string
  createdAt: string
}

export interface PaymentDetailDto {
  payment: PaymentSummaryDto
  failureReason: string | null
  deviceId: string
  processorRef: string | null
  order: OrderDto
  fraudAnalysis: FraudAnalysisDto | null
  refunds: RefundDto[]
  transitions: TransitionDto[]
  refundableAmount: number
}

export interface DashboardStats {
  totalPayments: number
  successful: number
  failed: number
  revenue: number
  refunded: number
}

export interface DashboardDaily {
  date: string
  captured: number
  failed: number
  revenue: number
}

export interface DashboardSummary {
  today: DashboardStats
  last7Days: DashboardStats
  daily: DashboardDaily[]
  recentAlerts: PaymentSummaryDto[]
}

export interface CreateOrderRequest {
  amount: number
  currency?: string
  notes?: string
  customerEmail?: string
  expiresInMinutes?: number
}

export interface CardDetails {
  number: string
  expiryMonth: number
  expiryYear: number
  holderName: string
}

export interface PaymentRequest {
  method: PaymentMethod
  customerEmail: string
  deviceId: string
  simulateOutcome?: ProcessorOutcome
  card?: CardDetails
  upiId?: string
  bankCode?: string
  walletProvider?: string
}
