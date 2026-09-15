import type { OrderStatus, PaymentStatus, RefundStatus, RiskLevel } from '../../types/api'

export function Badge({ children, tone = 'neutral' }: { children: React.ReactNode; tone?: 'success' | 'danger' | 'pending' | 'neutral' | 'primary' }) {
  const tones = {
    success: 'bg-emerald-50 text-emerald-700',
    danger: 'bg-rose-50 text-rose-700',
    pending: 'bg-amber-50 text-amber-700',
    neutral: 'bg-slate-100 text-slate-600',
    primary: 'bg-primary-light/40 text-primary-dark',
  }
  return <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${tones[tone]}`}>{children}</span>
}

export function StatusBadge({ status }: { status: PaymentStatus | OrderStatus | RefundStatus | RiskLevel }) {
  const labels: Record<string, string> = {
    CAPTURED: 'Successful',
    FAILED: 'Failed',
    REJECTED: 'Declined (risk)',
    VERIFICATION_REQUIRED: 'Needs verification',
    PROCESSING: 'Processing',
    PARTIALLY_REFUNDED: 'Partly refunded',
    REFUNDED: 'Refunded',
    CREATED: 'Awaiting payment',
    PAID: 'Paid',
    EXPIRED: 'Expired',
    PENDING: 'Pending',
    PROCESSED: 'Refunded',
    LOW: 'Low risk',
    MEDIUM: 'Medium risk',
    HIGH: 'High risk',
    INITIATED: 'Started',
    FRAUD_CHECK: 'Checking',
    AUTHORIZED: 'Authorized',
  }
  const tone = ['CAPTURED', 'PAID', 'PROCESSED', 'LOW'].includes(status) ? 'success' : ['FAILED', 'REJECTED', 'HIGH'].includes(status) ? 'danger' : ['PROCESSING', 'PENDING', 'VERIFICATION_REQUIRED', 'MEDIUM'].includes(status) ? 'pending' : 'neutral'
  return <Badge tone={tone}>{labels[status] ?? 'Pending'}</Badge>
}
