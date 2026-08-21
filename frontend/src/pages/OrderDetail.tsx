import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { apiErrorMessage, orderApi } from '../api/client'
import type { Order, Payment } from '../api/types'
import { RiskBadge, StatusBadge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { EmptyState, ErrorState, LoadingSpinner } from '../components/ui/Feedback'
import { Table } from '../components/ui/Table'
import { useToast } from '../components/ui/Toast'
import { formatDateTime, formatMoney } from '../lib/format'

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex justify-between gap-4 border-b border-slate-100 py-2 text-sm last:border-0">
      <span className="text-slate-500">{label}</span>
      <span className="text-right font-medium text-slate-800">{value ?? '—'}</span>
    </div>
  )
}

export default function OrderDetail() {
  const { orderId = '' } = useParams()
  const navigate = useNavigate()
  const { notify } = useToast()
  const [order, setOrder] = useState<Order | null>(null)
  const [payments, setPayments] = useState<Payment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    orderApi
      .get(orderId)
      .then((data) => {
        setOrder(data.order)
        setPayments(data.payments)
      })
      .catch((err) => setError(apiErrorMessage(err, 'Could not load the order')))
      .finally(() => setLoading(false))
  }, [orderId])

  if (loading) return <LoadingSpinner label="Loading order…" />
  if (error) return <ErrorState message={error} />
  if (!order) return null

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <Link to="/dashboard/orders" className="text-sm text-primary hover:text-primary-dark">
            ← Back to orders
          </Link>
          <h1 className="mt-1 text-xl font-semibold text-slate-800">
            {formatMoney(order.amount, order.currency)}
          </h1>
          <p className="font-mono text-xs text-slate-500">{order.id}</p>
        </div>
        <div className="flex items-center gap-2">
          <StatusBadge status={order.status} />
          <Button
            variant="secondary"
            onClick={() => {
              navigator.clipboard.writeText(order.paymentUrl)
              notify('Checkout link copied', 'success')
            }}
          >
            Copy checkout link
          </Button>
          <Button onClick={() => window.open(order.paymentUrl, '_blank', 'noopener')}>
            Open checkout
          </Button>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card title="Order">
          <DetailRow label="Receipt" value={order.receipt} />
          <DetailRow label="Description" value={order.description} />
          <DetailRow label="Notes" value={order.notes} />
          <DetailRow label="Created" value={formatDateTime(order.createdAt)} />
          <DetailRow label="Expires" value={formatDateTime(order.expiresAt)} />
          <DetailRow label="Paid at" value={formatDateTime(order.paidAt)} />
          <DetailRow label="Idempotency key" value={order.idempotencyKey} />
        </Card>

        <Card title="Customer">
          <DetailRow label="Name" value={order.customerName} />
          <DetailRow label="Email" value={order.customerEmail} />
          <DetailRow label="Phone" value={order.customerPhone} />
          <DetailRow
            label="Checkout link"
            value={<span className="break-all font-mono text-xs">{order.paymentUrl}</span>}
          />
        </Card>
      </div>

      <Card title="Payment attempts" bodyClassName="p-0">
        <Table
          rows={payments}
          rowKey={(row) => row.id}
          onRowClick={(row) => navigate(`/dashboard/transactions/${row.id}`)}
          empty={
            <div className="p-5">
              <EmptyState title="No attempts yet" description="Attempts appear once the customer opens the checkout." />
            </div>
          }
          columns={[
            { key: 'id', header: 'Transaction', render: (row) => <span className="font-mono text-xs">{row.id.slice(0, 8)}</span> },
            { key: 'method', header: 'Method', render: (row) => row.method.replace('_', ' ') },
            { key: 'amount', header: 'Amount', render: (row) => formatMoney(row.amount, row.currency) },
            { key: 'status', header: 'Status', render: (row) => <StatusBadge status={row.status} /> },
            { key: 'risk', header: 'Risk', render: (row) => <RiskBadge level={row.riskLevel} score={row.riskScore} /> },
            { key: 'created', header: 'Created', render: (row) => formatDateTime(row.createdAt) },
          ]}
        />
      </Card>
    </div>
  )
}
