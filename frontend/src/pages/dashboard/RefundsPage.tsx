import { useState } from 'react'
import { Link } from 'react-router-dom'
import { refundsApi } from '../../api/refunds'
import { useAsync } from '../../hooks/useAsync'
import { formatDateTime, formatMoney } from '../../lib/format'
import { Card } from '../../components/ui/Card'
import { EmptyState } from '../../components/ui/EmptyState'
import { LoadingSpinner } from '../../components/ui/LoadingSpinner'
import { PageHeader } from '../../components/ui/PageHeader'
import { Select } from '../../components/ui/Input'
import { StatusBadge } from '../../components/ui/Badge'
import { Table } from '../../components/ui/Table'
import { Button } from '../../components/ui/Button'
import type { RefundStatus } from '../../types/api'

export function RefundsPage() {
  const [status, setStatus] = useState<RefundStatus | ''>('')
  const result = useAsync(
    () =>
      refundsApi
        .list({ page: 0, size: 25, status: status || undefined })
        .then((response) => response.data),
    [status],
  )
  if (result.loading) return <LoadingSpinner />
  if (result.error)
    return (
      <div className="card text-center">
        <p className="text-sm text-rose-600">{result.error}</p>
        <Button className="mt-4" onClick={result.reload}>
          Try again
        </Button>
      </div>
    )
  return (
    <>
      <PageHeader title="Refunds" subtitle="Track refunds requested for your payments." />
      <Card>
        <div className="mb-5 max-w-xs">
          <Select
            label="Status"
            value={status}
            onChange={(e) => setStatus(e.target.value as RefundStatus | '')}
          >
            <option value="">All statuses</option>
            {['PENDING', 'PROCESSING', 'PROCESSED', 'FAILED'].map((value) => (
              <option key={value}>{value}</option>
            ))}
          </Select>
        </div>
        {result.data?.items.length ? (
          <Table
            rows={result.data.items}
            columns={[
              { key: 'id', header: 'Refund ID', render: (row) => row.id },
              {
                key: 'payment',
                header: 'Payment',
                render: (row) => (
                  <Link
                    className="text-primary-dark hover:underline"
                    to={`/dashboard/transactions/${row.paymentId}`}
                  >
                    {row.paymentId}
                  </Link>
                ),
              },
              {
                key: 'amount',
                header: 'Amount',
                render: (row) => formatMoney(row.amount, row.currency),
              },
              {
                key: 'status',
                header: 'Status',
                render: (row) => <StatusBadge status={row.status} />,
              },
              { key: 'reason', header: 'Reason', render: (row) => row.reason || '—' },
              {
                key: 'requested',
                header: 'Requested',
                render: (row) => formatDateTime(row.createdAt),
              },
              {
                key: 'processed',
                header: 'Processed',
                render: (row) => formatDateTime(row.processedAt),
              },
            ]}
          />
        ) : (
          <EmptyState
            title="No refunds yet"
            description="Refunds will appear here after you issue one from a transaction."
            action={
              <Link to="/dashboard/transactions">
                <Button variant="secondary">View transactions</Button>
              </Link>
            }
          />
        )}
      </Card>
    </>
  )
}
