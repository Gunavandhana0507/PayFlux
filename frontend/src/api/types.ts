export type PaymentMethod = 'CARD' | 'UPI' | 'NET_BANKING' | 'WALLET'

export type OrderStatus = 'CREATED' | 'ATTEMPTED' | 'PAID' | 'EXPIRED' | 'CANCELLED'

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

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'

export type MerchantFeedback = 'CONFIRMED_FRAUD' | 'FALSE_POSITIVE'

export interface MerchantProfile {
  merchantId: string
  userId: string
  email: string
  fullName: string
  role: 'MERCHANT' | 'ADMIN'
  businessName: string
  legalName?: string
  businessType?: string
  websiteUrl?: string
  contactName?: string
  contactPhone?: string
  panNumber?: string
  gstin?: string
  addressLine1?: string
  addressLine2?: string
  city?: string
  state?: string
  postalCode?: string
  country?: string
  bankAccountName?: string
  bankAccountNumber?: string
  bankIfsc?: string
  kycStatus: string
  apiKey?: string
}

export interface AuthResponse {
  token: string
  expiresIn: number
  profile: MerchantProfile
}

export interface RegisterPayload {
  email: string
  password: string
  fullName: string
  businessName: string
  legalName?: string
  businessType?: string
  websiteUrl?: string
  contactName: string
  contactPhone: string
  panNumber?: string
  gstin?: string
  addressLine1?: string
  addressLine2?: string
  city?: string
  state?: string
  postalCode?: string
  country?: string
  bankAccountName?: string
  bankAccountNumber?: string
  bankIfsc?: string
}

export interface Order {
  id: string
  merchantId: string
  amount: number
  currency: string
  receipt?: string
  description?: string
  notes?: string
  customerName?: string
  customerEmail?: string
  customerPhone?: string
  status: OrderStatus
  idempotencyKey?: string
  createdAt: string
  expiresAt?: string
  paidAt?: string
  paymentUrl: string
}

export interface CreateOrderPayload {
  amount: number
  currency: string
  receipt?: string
  description?: string
  notes?: string
  expiryMinutes?: number
  customerName?: string
  customerEmail?: string
  customerPhone?: string
}

export interface PublicOrder {
  id: string
  merchantName: string
  amount: number
  currency: string
  description?: string
  customerName?: string
  customerEmail?: string
  status: OrderStatus
  expiresAt?: string
  payable: boolean
}

export interface PublicPayment {
  id: string
  orderId: string
  status: PaymentStatus
  method: PaymentMethod
  amount: number
  currency: string
  nextAction?: string
  message?: string
  failureReason?: string
  createdAt: string
}

export interface InitiatePaymentPayload {
  orderId: string
  method: PaymentMethod
  cardNumber?: string
  cardHolderName?: string
  cardExpiry?: string
  cardCvv?: string
  upiVpa?: string
  bankCode?: string
  walletProvider?: string
  deviceFingerprint?: string
  simulateOutcome?: 'SUCCESS' | 'FAILURE' | 'TIMEOUT'
}

export interface Payment {
  id: string
  orderId: string
  orderReceipt?: string
  amount: number
  currency: string
  method: PaymentMethod
  status: PaymentStatus
  refundedAmount: number
  customerEmail?: string
  customerName?: string
  cardLast4?: string
  upiVpa?: string
  bankCode?: string
  walletProvider?: string
  processorReference?: string
  failureReason?: string
  attemptCount: number
  riskScore?: number
  riskLevel?: RiskLevel
  merchantFeedback?: MerchantFeedback
  createdAt: string
  capturedAt?: string
}

export interface RiskFactor {
  code: string
  description: string
  weight: number
}

export interface FraudAnalysis {
  id: string
  paymentId: string
  riskScore: number
  riskLevel: RiskLevel
  decision: 'ALLOW' | 'VERIFY' | 'REJECT'
  prediction: string
  modelVersion: string
  factors: RiskFactor[]
  features: Record<string, string>
  createdAt: string
  merchantFeedback?: MerchantFeedback
  feedbackNote?: string
  feedbackByEmail?: string
  feedbackAt?: string
}

export interface Transition {
  fromStatus?: PaymentStatus
  toStatus: PaymentStatus
  reason?: string
  actor?: string
  createdAt: string
}

export interface Refund {
  id: string
  paymentId: string
  orderId: string
  amount: number
  currency: string
  status: 'PENDING' | 'PROCESSING' | 'PROCESSED' | 'FAILED'
  reason?: string
  processorReference?: string
  initiatedBy?: string
  createdAt: string
  processedAt?: string
}

export interface PaymentDetail {
  payment: Payment
  fraudAnalysis?: FraudAnalysis
  transitions: Transition[]
  refunds: Refund[]
}

export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface DashboardSummary {
  capturedVolume: number
  refundedVolume: number
  successfulPayments: number
  failedPayments: number
  flaggedTransactions: number
  totalPayments: number
  series: { date: string; volume: number; count: number }[]
}
