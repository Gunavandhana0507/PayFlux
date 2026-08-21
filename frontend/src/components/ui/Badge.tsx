import clsx from 'clsx'
import type { ReactNode } from 'react'

export type BadgeTone = 'neutral' | 'primary' | 'success' | 'warning' | 'danger'

const tones: Record<BadgeTone, string> = {
  neutral: 'bg-slate-100 text-slate-600 border-slate-200',
  primary: 'bg-primary-light/40 text-primary-dark border-primary-light',
  success: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  warning: 'bg-amber-50 text-amber-700 border-amber-200',
  danger: 'bg-rose-50 text-rose-700 border-rose-200',
}

export function Badge({
  tone = 'neutral',
  children,
  className,
}: {
  tone?: BadgeTone
  children: ReactNode
  className?: string
}) {
  return (
    <span
      className={clsx(
        'inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium',
        tones[tone],
        className,
      )}
    >
      {children}
    </span>
  )
}

const statusTones: Record<string, BadgeTone> = {
  CAPTURED: 'success',
  PAID: 'success',
  PROCESSED: 'success',
  AUTHORIZED: 'primary',
  PROCESSING: 'primary',
  PENDING: 'warning',
  CREATED: 'neutral',
  INITIATED: 'neutral',
  ATTEMPTED: 'warning',
  FRAUD_CHECK: 'warning',
  VERIFICATION_REQUIRED: 'warning',
  PARTIALLY_REFUNDED: 'warning',
  REFUNDED: 'neutral',
  EXPIRED: 'neutral',
  CANCELLED: 'neutral',
  FAILED: 'danger',
  REJECTED: 'danger',
}

export function StatusBadge({ status }: { status: string }) {
  return <Badge tone={statusTones[status] ?? 'neutral'}>{status.replace(/_/g, ' ')}</Badge>
}

export function RiskBadge({ level, score }: { level?: string; score?: number }) {
  if (!level) return <span className="text-slate-400">—</span>
  const tone: BadgeTone = level === 'HIGH' ? 'danger' : level === 'MEDIUM' ? 'warning' : 'success'
  return (
    <Badge tone={tone}>
      {level}
      {score !== undefined && score !== null ? ` · ${score}` : ''}
    </Badge>
  )
}
