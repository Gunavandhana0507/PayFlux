import { Link, useParams } from 'react-router-dom'
import { ordersApi } from '../../api/orders'
import { useAsync } from '../../hooks/useAsync'
import { formatDateTime, formatMoney } from '../../lib/format'
import { Card } from '../../components/ui/Card'
import { EmptyState } from '../../components/ui/EmptyState'
import { LoadingSpinner } from '../../components/ui/LoadingSpinner'
import { PageHeader } from '../../components/ui/PageHeader'
import { StatusBadge } from '../../components/ui/Badge'
import { Table } from '../../components/ui/Table'

export function OrderDetailPage() {
  const { id = '' } = useParams()
  const result = useAsync(() => ordersApi.detail(id).then((response) => response.data), [id])
  if (result.loading) return <LoadingSpinner />
  if (result.error || !result.data) return <EmptyState title="We couldn't load this order" description={result.error} />
  const { order, payments } = result.data
  return <><PageHeader title={order.id} subtitle={<Link to="/dashboard/orders" className="text-primary-dark hover:underline">Orders</Link>} /><div className="grid gap-6 lg:grid-cols-2"><Card title="Order details"><dl className="grid grid-cols-2 gap-4 text-sm"><div><dt className="text-slate-500">Amount</dt><dd className="mt-1 font-medium">{formatMoney(order.amount, order.currency)}</dd></div><div><dt className="text-slate-500">Status</dt><dd className="mt-1"><StatusBadge status={order.status} /></dd></div><div><dt className="text-slate-500">Created</dt><dd className="mt-1">{formatDateTime(order.createdAt)}</dd></div><div><dt className="text-slate-500">Expires</dt><dd className="mt-1">{formatDateTime(order.expiresAt)}</dd></div></dl>{order.notes && <p className="mt-5 border-t border-slate-100 pt-4 text-sm text-slate-600">{order.notes}</p>}<div className="mt-5 rounded-md bg-slate-50 p-3 text-xs break-all">{order.paymentUrl}</div></Card><Card title="Payment attempts">{payments.length ? <Table rows={payments} columns={[{ key: 'id', header: 'Payment', render: (payment) => <Link className="text-primary-dark hover:underline" to={`/dashboard/transactions/${payment.id}`}>{payment.id}</Link> }, { key: 'amount', header: 'Amount', render: (payment) => formatMoney(payment.amount, payment.currency) }, { key: 'status', header: 'Status', render: (payment) => <StatusBadge status={payment.status} /> }, { key: 'risk', header: 'Risk', render: (payment) => payment.riskLevel ? <StatusBadge status={payment.riskLevel} /> : '—' }]} /> : <EmptyState title="No payment attempts yet" />}</Card></div></>
}
