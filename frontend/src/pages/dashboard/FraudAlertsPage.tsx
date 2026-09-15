import { useState } from 'react'
import { Link } from 'react-router-dom'
import { fraudApi } from '../../api/fraud'
import { useAsync } from '../../hooks/useAsync'
import { formatMoney, formatDateTime } from '../../lib/format'
import { Card } from '../../components/ui/Card'
import { EmptyState } from '../../components/ui/EmptyState'
import { LoadingSpinner } from '../../components/ui/LoadingSpinner'
import { PageHeader } from '../../components/ui/PageHeader'
import { Pagination } from '../../components/ui/Pagination'
import { StatusBadge } from '../../components/ui/Badge'
import { Table } from '../../components/ui/Table'
import { Button } from '../../components/ui/Button'

export function FraudAlertsPage() {
  const [page, setPage] = useState(0)
  const result = useAsync(() => fraudApi.list({ page, size: 10 }).then((response) => response.data), [page])
  if (result.loading) return <LoadingSpinner />
  if (result.error) return <div className="card text-center"><p className="text-sm text-rose-600">{result.error}</p><Button className="mt-4" onClick={result.reload}>Try again</Button></div>
  return <><PageHeader title="Fraud alerts" subtitle="Review medium and high risk payments." /><Card>{result.data?.items.length ? <Table rows={result.data.items} columns={[{ key: 'payment', header: 'Payment', render: (row) => <Link to={`/dashboard/transactions/${row.id}`} className="text-primary-dark hover:underline">{row.id}</Link> }, { key: 'customer', header: 'Customer', render: (row) => row.customerEmail }, { key: 'amount', header: 'Amount', render: (row) => formatMoney(row.amount, row.currency) }, { key: 'risk', header: 'Risk', render: (row) => row.riskLevel ? <StatusBadge status={row.riskLevel} /> : '—' }, { key: 'feedback', header: 'Feedback', render: () => '—' }, { key: 'time', header: 'Time', render: (row) => formatDateTime(row.createdAt) }]} /> : <EmptyState title="No flagged payments" description="We'll list any medium or high risk payments here." />}<Pagination page={result.data?.page ?? 0} size={result.data?.size ?? 10} total={result.data?.total ?? 0} onChange={setPage} /></Card></>
}
