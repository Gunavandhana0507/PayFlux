import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { apiErrorMessage, fraudApi, paymentApi } from '../api/client'
import type { MerchantFeedback, PaymentDetail } from '../api/types'
import { Badge, RiskBadge, StatusBadge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { EmptyState, ErrorState, LoadingSpinner } from '../components/ui/Feedback'
import { Input, Textarea } from '../components/ui/Input'
import { Modal } from '../components/ui/Modal'
import { Table } from '../components/ui/Table'
import { useToast } from '../components/ui/Toast'
import { formatDateTime, formatMoney, humanizeFeatureName } from '../lib/format'

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex justify-between gap-4 border-b border-slate-100 py-2 text-sm last:border-0">
      <span className="text-slate-500">{label}</span>
      <span className="text-right font-medium text-slate-800">{value ?? '—'}</span>
    </div>
  )
}

export default function TransactionDetail() {
  const { paymentId = '' } = useParams()
  const { notify } = useToast()
  const [detail, setDetail] = useState<PaymentDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [feedbackNote, setFeedbackNote] = useState('')
  const [savingFeedback, setSavingFeedback] = useState<MerchantFeedback | null>(null)
  const [refundOpen, setRefundOpen] = useState(false)
  const [refundAmount, setRefundAmount] = useState('')
  const [refundReason, setRefundReason] = useState('')
  const [refunding, setRefunding] = useState(false)

  const load = useCallback(() => {
    paymentApi
      .detail(paymentId)
      .then(setDetail)
      .catch((err) => setError(apiErrorMessage(err, 'Could not load the transaction')))
      .finally(() => setLoading(false))
  }, [paymentId])

  useEffect(load, [load])

  async function submitFeedback(feedback: MerchantFeedback) {
    if (!detail?.fraudAnalysis) return
    setSavingFeedback(feedback)
    try {
      const updated = await fraudApi.feedback(detail.fraudAnalysis.id, feedback, feedbackNote || undefined)
      setDetail({ ...detail, fraudAnalysis: updated })
      setFeedbackNote('')
      notify('Feedback saved for model retraining', 'success')
    } catch (err) {
      notify(apiErrorMessage(err, 'Could not save the feedback'), 'error')
    } finally {
      setSavingFeedback(null)
    }
  }

  async function submitRefund(event: FormEvent) {
    event.preventDefault()
    setRefunding(true)
    try {
      await paymentApi.refund(paymentId, refundAmount ? Number(refundAmount) : null, refundReason)
      notify('Refund queued', 'success')
      setRefundOpen(false)
      setRefundAmount('')
      setRefundReason('')
      window.setTimeout(load, 2500)
    } catch (err) {
      notify(apiErrorMessage(err, 'Could not create the refund'), 'error')
    } finally {
      setRefunding(false)
    }
  }

  if (loading) return <LoadingSpinner label="Loading transaction…" />
  if (error) return <ErrorState message={error} />
  if (!detail) return null

  const { payment, fraudAnalysis, transitions, refunds } = detail
  const refundable = Number(payment.amount) - Number(payment.refundedAmount ?? 0)
  const canRefund =
    (payment.status === 'CAPTURED' || payment.status === 'PARTIALLY_REFUNDED') && refundable > 0

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <Link to="/dashboard/transactions" className="text-sm text-primary hover:text-primary-dark">
            ← Back to transactions
          </Link>
          <h1 className="mt-1 text-xl font-semibold text-slate-800">
            {formatMoney(payment.amount, payment.currency)}
          </h1>
          <p className="font-mono text-xs text-slate-500">{payment.id}</p>
        </div>
        <div className="flex items-center gap-2">
          <StatusBadge status={payment.status} />
          <RiskBadge level={payment.riskLevel} score={payment.riskScore} />
          {canRefund && <Button onClick={() => setRefundOpen(true)}>Refund</Button>}
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card title="Payment">
          <DetailRow label="Order" value={<Link className="text-primary" to={`/dashboard/orders/${payment.orderId}`}>{payment.orderId.slice(0, 8)}</Link>} />
          <DetailRow label="Method" value={payment.method.replace('_', ' ')} />
          <DetailRow
            label="Instrument"
            value={payment.cardLast4 ? `•••• ${payment.cardLast4}` : payment.upiVpa ?? payment.bankCode ?? payment.walletProvider}
          />
          <DetailRow label="Refunded" value={formatMoney(payment.refundedAmount, payment.currency)} />
          <DetailRow label="Processor reference" value={payment.processorReference} />
          <DetailRow label="Failure reason" value={payment.failureReason} />
          <DetailRow label="Attempts" value={payment.attemptCount} />
          <DetailRow label="Created" value={formatDateTime(payment.createdAt)} />
          <DetailRow label="Captured" value={formatDateTime(payment.capturedAt)} />
        </Card>

        <Card title="Customer">
          <DetailRow label="Name" value={payment.customerName} />
          <DetailRow label="Email" value={payment.customerEmail} />
          <DetailRow label="Receipt" value={payment.orderReceipt} />
        </Card>
      </div>

      {fraudAnalysis && (
        <Card
          title="Risk assessment"
          description={`${fraudAnalysis.decision} · model ${fraudAnalysis.modelVersion} · ${fraudAnalysis.prediction.replace(/_/g, ' ').toLowerCase()}`}
          actions={<RiskBadge level={fraudAnalysis.riskLevel} score={fraudAnalysis.riskScore} />}
        >
          <div className="space-y-6">
            <div>
              <h3 className="text-sm font-semibold text-slate-700">Contributing factors</h3>
              <ul className="mt-2 space-y-2">
                {fraudAnalysis.factors.map((factor) => (
                  <li
                    key={factor.code}
                    className="flex items-start justify-between gap-4 rounded-lg border border-slate-200 bg-slate-50/60 px-4 py-3"
                  >
                    <div>
                      <p className="text-sm text-slate-700">{factor.description}</p>
                      <p className="mt-0.5 font-mono text-xs text-slate-400">{factor.code}</p>
                    </div>
                    <Badge tone="primary">+{factor.weight}</Badge>
                  </li>
                ))}
                {fraudAnalysis.factors.length === 0 && (
                  <li className="text-sm text-slate-500">No risk factors were triggered.</li>
                )}
              </ul>
            </div>

            <div>
              <h3 className="text-sm font-semibold text-slate-700">Features evaluated</h3>
              <dl className="mt-2 grid gap-x-6 gap-y-1 sm:grid-cols-2">
                {Object.entries(fraudAnalysis.features ?? {}).map(([name, value]) => (
                  <div key={name} className="flex justify-between border-b border-slate-100 py-1.5 text-sm">
                    <dt className="text-slate-500">{humanizeFeatureName(name)}</dt>
                    <dd className="font-medium text-slate-700">{value}</dd>
                  </div>
                ))}
              </dl>
            </div>

            <div className="rounded-lg border border-slate-200 p-4">
              <h3 className="text-sm font-semibold text-slate-700">Merchant review</h3>
              {fraudAnalysis.merchantFeedback ? (
                <div className="mt-2 space-y-1 text-sm text-slate-600">
                  <Badge tone={fraudAnalysis.merchantFeedback === 'CONFIRMED_FRAUD' ? 'danger' : 'success'}>
                    {fraudAnalysis.merchantFeedback.replace(/_/g, ' ')}
                  </Badge>
                  {fraudAnalysis.feedbackNote && <p>“{fraudAnalysis.feedbackNote}”</p>}
                  <p className="text-xs text-slate-500">
                    Submitted by {fraudAnalysis.feedbackByEmail} on {formatDateTime(fraudAnalysis.feedbackAt)}
                  </p>
                  <p className="text-xs text-slate-500">
                    Submitting again overwrites this verdict.
                  </p>
                </div>
              ) : (
                <p className="mt-1 text-sm text-slate-500">
                  Tell the risk engine whether this decision was correct. Your verdict is stored with the
                  original feature set for future retraining.
                </p>
              )}

              <div className="mt-3 space-y-3">
                <Textarea
                  label="Note (optional)"
                  rows={2}
                  value={feedbackNote}
                  onChange={(event) => setFeedbackNote(event.target.value)}
                />
                <div className="flex flex-wrap gap-2">
                  <Button
                    variant="danger"
                    loading={savingFeedback === 'CONFIRMED_FRAUD'}
                    onClick={() => submitFeedback('CONFIRMED_FRAUD')}
                  >
                    Confirm Fraud
                  </Button>
                  <Button
                    variant="secondary"
                    loading={savingFeedback === 'FALSE_POSITIVE'}
                    onClick={() => submitFeedback('FALSE_POSITIVE')}
                  >
                    Mark as False Positive
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </Card>
      )}

      <Card title="Refunds" bodyClassName="p-0">
        <Table
          rows={refunds}
          rowKey={(row) => row.id}
          empty={
            <div className="p-5">
              <EmptyState title="No refunds" description="Refunds you issue against this payment appear here." />
            </div>
          }
          columns={[
            { key: 'id', header: 'Refund', render: (row) => <span className="font-mono text-xs">{row.id.slice(0, 8)}</span> },
            { key: 'amount', header: 'Amount', render: (row) => formatMoney(row.amount, row.currency) },
            { key: 'status', header: 'Status', render: (row) => <StatusBadge status={row.status} /> },
            { key: 'reason', header: 'Reason', render: (row) => row.reason ?? '—' },
            { key: 'created', header: 'Created', render: (row) => formatDateTime(row.createdAt) },
            { key: 'processed', header: 'Processed', render: (row) => formatDateTime(row.processedAt) },
          ]}
        />
      </Card>

      <Card title="State machine" description="Every transition recorded for this payment.">
        <ol className="space-y-3">
          {transitions.map((transition, index) => (
            <li key={`${transition.toStatus}-${index}`} className="flex items-start gap-3">
              <span className="mt-1 h-2 w-2 shrink-0 rounded-full bg-primary" />
              <div>
                <p className="text-sm font-medium text-slate-700">
                  {transition.fromStatus ? `${transition.fromStatus} → ` : ''}
                  {transition.toStatus}
                </p>
                <p className="text-xs text-slate-500">
                  {transition.reason} · {transition.actor} · {formatDateTime(transition.createdAt)}
                </p>
              </div>
            </li>
          ))}
        </ol>
      </Card>

      <Modal
        open={refundOpen}
        title="Issue refund"
        description={`Refundable balance: ${formatMoney(refundable, payment.currency)}`}
        onClose={() => setRefundOpen(false)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setRefundOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" form="refund-form" loading={refunding}>
              Refund
            </Button>
          </>
        }
      >
        <form id="refund-form" className="space-y-4" onSubmit={submitRefund}>
          <Input
            label="Amount"
            type="number"
            min="0.01"
            step="0.01"
            max={refundable}
            placeholder={String(refundable)}
            hint="Leave empty to refund the full remaining balance."
            value={refundAmount}
            onChange={(event) => setRefundAmount(event.target.value)}
          />
          <Input
            label="Reason"
            value={refundReason}
            onChange={(event) => setRefundReason(event.target.value)}
          />
        </form>
      </Modal>
    </div>
  )
}
