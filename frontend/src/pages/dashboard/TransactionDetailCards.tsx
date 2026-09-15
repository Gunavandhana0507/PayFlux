import { useEffect, useState } from 'react'
import type { PaymentDetailDto } from '../../types/api'
import { paymentsApi } from '../../api/payments'
import { formatDateTime, formatMoney } from '../../lib/format'
import { Card } from '../../components/ui/Card'
import { StatusBadge, statusLabels } from '../../components/ui/Badge'
import { RiskFactorsPanel } from '../../components/RiskFactorsPanel'
import { FeedbackButtons } from '../../components/FeedbackButtons'
import { Button } from '../../components/ui/Button'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { Input, Textarea } from '../../components/ui/Input'
import { useToast } from '../../components/ui/Toast'

export function SummaryCard({ detail }: { detail: PaymentDetailDto }) {
  return (
    <Card title="Payment summary">
      <dl className="grid grid-cols-2 gap-4 text-sm">
        <div>
          <dt className="text-slate-500">Amount</dt>
          <dd className="mt-1 font-medium">
            {formatMoney(detail.payment.amount, detail.payment.currency)}
          </dd>
        </div>
        <div>
          <dt className="text-slate-500">Status</dt>
          <dd className="mt-1">
            <StatusBadge status={detail.payment.status} />
          </dd>
        </div>
        <div>
          <dt className="text-slate-500">Method</dt>
          <dd className="mt-1">{detail.payment.methodSummary}</dd>
        </div>
        <div>
          <dt className="text-slate-500">Created</dt>
          <dd className="mt-1">{formatDateTime(detail.payment.createdAt)}</dd>
        </div>
      </dl>
      {detail.failureReason && (
        <p className="mt-5 rounded-md bg-rose-50 p-3 text-sm text-rose-700">
          {detail.failureReason}
        </p>
      )}
    </Card>
  )
}

export function RiskAssessmentCard({
  detail,
  onFeedback,
}: {
  detail: PaymentDetailDto
  onFeedback: (feedback: 'CONFIRMED_FRAUD' | 'FALSE_POSITIVE', note?: string) => Promise<void>
}) {
  const analysis = detail.fraudAnalysis
  return (
    <Card title="Risk assessment">
      {!analysis ? (
        <p className="text-sm text-slate-500">Risk assessment is not available yet.</p>
      ) : (
        <div className="space-y-5">
          <div className="flex items-center justify-between gap-4">
            <StatusBadge status={analysis.riskLevel} />
            <span className="text-right text-sm text-slate-500">
              Model: {analysis.modelVersion} · This is a risk estimate, not proof of fraud.
            </span>
          </div>
          {analysis.riskLevel === 'LOW' ? (
            <p className="text-sm text-slate-600">Nothing unusual was found for this payment.</p>
          ) : (
            <>
              <RiskFactorsPanel analysis={analysis} />
              <FeedbackButtons
                feedback={analysis.merchantFeedback}
                feedbackAt={analysis.feedbackAt}
                feedbackNote={analysis.feedbackNote}
                onFeedback={onFeedback}
              />
            </>
          )}
        </div>
      )}
    </Card>
  )
}

export function RefundsCard({
  detail,
  onReload,
}: {
  detail: PaymentDetailDto
  onReload: () => Promise<void>
}) {
  const toast = useToast()
  const [amount, setAmount] = useState(String(detail.refundableAmount))
  const [reason, setReason] = useState('')
  const [formError, setFormError] = useState('')
  const [confirmOpen, setConfirmOpen] = useState(false)
  const refundable =
    detail.payment.status === 'CAPTURED' || detail.payment.status === 'PARTIALLY_REFUNDED'
  const submit = async () => {
    const value = Number(amount)
    if (!amount || !Number.isFinite(value) || value <= 0) {
      setFormError('Enter a refund amount.')
      throw new Error('invalid refund amount')
    }
    if (value > detail.refundableAmount) {
      setFormError(`Amount cannot be more than ${formatMoney(detail.refundableAmount)}.`)
      throw new Error('refund exceeds remaining amount')
    }
    setFormError('')
    await paymentsApi.refund(detail.payment.id, { amount: value, reason: reason || undefined })
    setAmount('')
    setReason('')
    toast.show(`Refund of ${formatMoney(value)} started`, 'success')
    await onReload()
  }

  useEffect(() => {
    const hasPending = detail.refunds.some(
      (refund) => refund.status === 'PENDING' || refund.status === 'PROCESSING',
    )
    if (!hasPending) return
    const timer = window.setInterval(() => void onReload(), 3000)
    return () => window.clearInterval(timer)
  }, [detail.refunds, onReload])

  return (
    <Card title="Refunds">
      <p className="text-sm text-slate-600">
        Refundable amount:{' '}
        <strong>{formatMoney(detail.refundableAmount, detail.payment.currency)}</strong>
      </p>
      {refundable && detail.refundableAmount > 0 && (
        <div className="mt-4 space-y-3">
          <Input
            label="Refund amount"
            type="number"
            min="0.01"
            max={detail.refundableAmount}
            value={amount}
            error={formError}
            onChange={(event) => {
              setAmount(event.target.value)
              setFormError('')
            }}
          />
          <button
            type="button"
            className="text-sm font-medium text-primary-dark hover:underline"
            onClick={() => setAmount(String(detail.refundableAmount))}
          >
            Full refund
          </button>
          <Textarea
            label="Reason (optional)"
            value={reason}
            onChange={(event) => setReason(event.target.value)}
          />
          <Button size="sm" onClick={() => setConfirmOpen(true)}>
            Issue refund
          </Button>
        </div>
      )}
      {detail.refunds.length > 0 && (
        <div className="mt-5 divide-y divide-slate-100">
          {detail.refunds.map((refund) => (
            <div key={refund.id} className="flex justify-between py-3 text-sm">
              <span>{formatMoney(refund.amount, refund.currency)}</span>
              <StatusBadge status={refund.status} />
            </div>
          ))}
        </div>
      )}
      <ConfirmDialog
        open={confirmOpen}
        title="Confirm refund"
        description={`Start a refund of ${formatMoney(Number(amount) || 0, detail.payment.currency)}?`}
        onClose={() => setConfirmOpen(false)}
        onConfirm={submit}
      />
    </Card>
  )
}

export function TimelineCard({ detail }: { detail: PaymentDetailDto }) {
  const actors = { SYSTEM: 'System', CUSTOMER: 'Customer', MERCHANT: 'Merchant' }
  return (
    <Card title="Payment timeline">
      <div className="space-y-4">
        {detail.transitions.map((transition) => (
          <div
            key={`${transition.createdAt}-${transition.toStatus}`}
            className="flex gap-3 text-sm"
          >
            <div className="mt-1 h-2 w-2 rounded-full bg-primary" />
            <div>
              <p className="font-medium">
                {statusLabels[transition.fromStatus] ?? transition.fromStatus} →{' '}
                <StatusBadge status={transition.toStatus} />
              </p>
              <p className="mt-1 text-slate-500">
                {actors[transition.actor]} · {transition.reason} ·{' '}
                {formatDateTime(transition.createdAt)}
              </p>
            </div>
          </div>
        ))}
      </div>
    </Card>
  )
}
