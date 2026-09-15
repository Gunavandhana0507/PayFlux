import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { paymentsApi } from '../../api/payments'
import { useAsync } from '../../hooks/useAsync'
import { formatDateTime, formatMoney } from '../../lib/format'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { EmptyState } from '../../components/ui/EmptyState'
import { LoadingSpinner } from '../../components/ui/LoadingSpinner'
import { PageHeader } from '../../components/ui/PageHeader'
import { Select } from '../../components/ui/Input'
import { StatusBadge } from '../../components/ui/Badge'
import { Table } from '../../components/ui/Table'
import type { PaymentMethod, PaymentStatus, RiskLevel } from '../../types/api'

export function TransactionsPage() {
  const [filters, setFilters] = useState<{
    status: PaymentStatus | ''
    riskLevel: RiskLevel | ''
    method: PaymentMethod | ''
  }>({ status: '', riskLevel: '', method: '' })
  const navigate = useNavigate()
  const result = useAsync(
    () =>
      paymentsApi
        .list({
          page: 0,
          size: 25,
          ...Object.fromEntries(
            Object.entries(filters).map(([key, value]) => [key, value || undefined]),
          ),
        } as never)
        .then((response) => response.data),
    [filters],
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
      <PageHeader title="Transactions" subtitle="Review every payment attempt." />
      <Card>
        <div className="mb-5 grid gap-3 sm:grid-cols-3">
          <Select
            label="Status"
            value={filters.status}
            onChange={(e) =>
              setFilters({ ...filters, status: e.target.value as PaymentStatus | '' })
            }
          >
            <option value="">All statuses</option>
            {['CAPTURED', 'FAILED', 'REJECTED', 'PROCESSING', 'REFUNDED'].map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </Select>
          <Select
            label="Risk"
            value={filters.riskLevel}
            onChange={(e) =>
              setFilters({ ...filters, riskLevel: e.target.value as RiskLevel | '' })
            }
          >
            <option value="">All risk</option>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
          </Select>
          <Select
            label="Method"
            value={filters.method}
            onChange={(e) =>
              setFilters({ ...filters, method: e.target.value as PaymentMethod | '' })
            }
          >
            <option value="">All methods</option>
            {['CARD', 'UPI', 'NETBANKING', 'WALLET'].map((value) => (
              <option key={value}>{value}</option>
            ))}
          </Select>
        </div>
        {result.data?.items.length ? (
          <Table
            rows={result.data.items}
            onRowClick={(row) => navigate(`/dashboard/transactions/${row.id}`)}
            columns={[
              { key: 'id', header: 'Payment ID', render: (row) => row.id },
              { key: 'order', header: 'Order', render: (row) => row.orderId },
              { key: 'customer', header: 'Customer', render: (row) => row.customerEmail },
              { key: 'method', header: 'Method', render: (row) => row.methodSummary },
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
              {
                key: 'risk',
                header: 'Risk',
                render: (row) => (row.riskLevel ? <StatusBadge status={row.riskLevel} /> : '—'),
              },
              { key: 'time', header: 'Time', render: (row) => formatDateTime(row.createdAt) },
            ]}
          />
        ) : (
          <EmptyState
            title="No transactions found"
            description="Try adjusting your filters."
            action={
              <Button variant="secondary" onClick={() => navigate('/dashboard/orders')}>
                Create an order
              </Button>
            }
          />
        )}
      </Card>
    </>
  )
}
