import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { fraudApi } from '../../api/fraud'
import { paymentsApi } from '../../api/payments'
import { useAsync } from '../../hooks/useAsync'
import { formatDateTime, formatMoney } from '../../lib/format'
import { Card } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { LoadingSpinner } from '../../components/ui/LoadingSpinner'
import { PageHeader } from '../../components/ui/PageHeader'
import { StatusBadge } from '../../components/ui/Badge'
import { RiskFactorsPanel } from '../../components/RiskFactorsPanel'
import { FeedbackButtons } from '../../components/FeedbackButtons'
import { useToast } from '../../components/ui/Toast'

export function TransactionDetailPage() {
  const { id = '' } = useParams()
  const toast = useToast()
  const [refundAmount, setRefundAmount] = useState('')
  const result = useAsync(() => paymentsApi.detail(id).then((response) => response.data), [id])
  if (result.loading) return <LoadingSpinner />
  if (result.error || !result.data) return <div className="card text-center text-sm text-rose-600">{result.error || 'Transaction unavailable'}</div>
  const detail = result.data
  const analysis = detail.fraudAnalysis
  const submitFeedback = async (feedback: 'CONFIRMED_FRAUD' | 'FALSE_POSITIVE', note?: string) => { await fraudApi.feedback(id, { feedback, note }); toast.show('Thanks — this helps improve future fraud detection', 'success'); void result.reload() }
  const issueRefund = async () => { const amount = Number(refundAmount); if (!amount || amount > detail.refundableAmount) return; await paymentsApi.refund(id, { amount }); setRefundAmount(''); toast.show(`Refund of ${formatMoney(amount)} started`, 'success'); void result.reload() }
  return <><PageHeader title={detail.payment.id} subtitle={`Order ${detail.order.id}`} /><div className="grid gap-6 lg:grid-cols-2"><Card title="Payment summary"><dl className="grid grid-cols-2 gap-4 text-sm"><div><dt className="text-slate-500">Amount</dt><dd className="mt-1 font-medium">{formatMoney(detail.payment.amount, detail.payment.currency)}</dd></div><div><dt className="text-slate-500">Status</dt><dd className="mt-1"><StatusBadge status={detail.payment.status} /></dd></div><div><dt className="text-slate-500">Method</dt><dd className="mt-1">{detail.payment.methodSummary}</dd></div><div><dt className="text-slate-500">Created</dt><dd className="mt-1">{formatDateTime(detail.payment.createdAt)}</dd></div></dl>{detail.failureReason && <p className="mt-5 rounded-md bg-rose-50 p-3 text-sm text-rose-700">{detail.failureReason}</p>}</Card><Card title="Risk assessment">{analysis ? <div className="space-y-5"><div className="flex items-center justify-between"><div><StatusBadge status={analysis.riskLevel} /><p className="mt-2 text-sm text-slate-500">Model: {analysis.modelVersion} · This is a risk estimate, not proof of fraud.</p></div><span className="text-3xl font-semibold text-slate-900">{Number(analysis.riskScore).toFixed(2)}</span></div>{analysis.riskLevel === 'LOW' ? <p className="text-sm text-slate-600">Nothing unusual was found for this payment.</p> : <><RiskFactorsPanel analysis={analysis} /><FeedbackButtons onFeedback={submitFeedback} /></>}</div> : <p className="text-sm text-slate-500">Risk assessment is not available yet.</p>}</Card><Card title="Refunds"><p className="text-sm text-slate-600">Refundable amount: <strong>{formatMoney(detail.refundableAmount, detail.payment.currency)}</strong></p>{detail.refundableAmount > 0 && ['CAPTURED', 'PARTIALLY_REFUNDED'].includes(detail.payment.status) && <div className="mt-4 flex gap-2"><input className="w-40 rounded-md border border-slate-300 px-3 py-2 text-sm" type="number" min="0.01" max={detail.refundableAmount} value={refundAmount} onChange={(e) => setRefundAmount(e.target.value)} placeholder="Amount" /><Button size="sm" onClick={() => void issueRefund()}>Issue refund</Button></div>}{detail.refunds.length > 0 && <div className="mt-5 divide-y divide-slate-100">{detail.refunds.map((refund) => <div key={refund.id} className="flex justify-between py-3 text-sm"><span>{formatMoney(refund.amount, refund.currency)}</span><StatusBadge status={refund.status} /></div>)}</div>}</Card><Card title="Payment timeline"><div className="space-y-4">{detail.transitions.map((transition) => <div key={`${transition.createdAt}-${transition.toStatus}`} className="flex gap-3 text-sm"><div className="mt-1 h-2 w-2 rounded-full bg-primary" /><div><p className="font-medium"><StatusBadge status={transition.toStatus} /> <span className="ml-2 text-slate-500">{transition.actor.toLowerCase()}</span></p><p className="mt-1 text-slate-500">{transition.reason} · {formatDateTime(transition.createdAt)}</p></div></div>)}</div></Card></div></>
}
