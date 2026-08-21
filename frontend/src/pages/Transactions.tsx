import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiErrorMessage, paymentApi } from '../api/client'
import type { Payment } from '../api/types'
import { Badge, RiskBadge, StatusBadge } from '../components/ui/Badge'
import { Card } from '../components/ui/Card'
import { EmptyState, ErrorState, LoadingSpinner } from '../components/ui/Feedback'
import { Select } from '../components/ui/Input'
import { Table } from '../components/ui/Table'
import { formatDateTime, formatMoney } from '../lib/format'

const statuses = [
  'CREATED',
  'INITIATED',
  'FRAUD_CHECK',
  'AUTHORIZED',
  'VERIFICATION_REQUIRED',
  'PROCESSING',
  'CAPTURED',
  'FAILED',
  'REJECTED',
  'PARTIALLY_REFUNDED',
  'REFUNDED',
]

export default function Transactions() {
  const navigate = useNavigate()
  const [rows, setRows] = useState<Payment[]>([])
  const [status, setStatus] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    setLoading(true)
    paymentApi
      .list({ status: status || undefined, size: 50 })
      .then((page) => setRows(page.items))
      .catch((err) => setError(apiErrorMessage(err, 'Could not load transactions')))
      .finally(() => setLoading(false))
  }, [status])

  useEffect(load, [load])

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold text-slate-800">Transactions</h1>
          <p className="text-sm text-slate-500">Every payment attempt with its risk assessment.</p>
        </div>
        <Select
          label="Status"
          className="w-56"
          value={status}
          onChange={(event) => setStatus(event.target.value)}
        >
          <option value="">All statuses</option>
          {statuses.map((value) => (
            <option key={value} value={value}>
              {value.replace(/_/g, ' ')}
            </option>
          ))}
        </Select>
      </div>

      {error && <ErrorState message={error} />}

      <Card bodyClassName="p-0">
        {loading ? (
          <LoadingSpinner label="Loading transactions…" />
        ) : (
          <Table
            rows={rows}
            rowKey={(row) => row.id}
            onRowClick={(row) => navigate(`/dashboard/transactions/${row.id}`)}
            empty={
              <div className="p-5">
                <EmptyState
                  title="No transactions"
                  description="Complete a payment on a checkout link to see it here."
                />
              </div>
            }
            columns={[
              { key: 'id', header: 'Transaction', render: (row) => <span className="font-mono text-xs">{row.id.slice(0, 8)}</span> },
              { key: 'customer', header: 'Customer', render: (row) => row.customerName ?? row.customerEmail ?? '—' },
              { key: 'amount', header: 'Amount', render: (row) => formatMoney(row.amount, row.currency) },
              { key: 'method', header: 'Method', render: (row) => row.method.replace('_', ' ') },
              { key: 'status', header: 'Status', render: (row) => <StatusBadge status={row.status} /> },
              { key: 'risk', header: 'Risk', render: (row) => <RiskBadge level={row.riskLevel} score={row.riskScore} /> },
              {
                key: 'feedback',
                header: 'Feedback',
                render: (row) =>
                  row.merchantFeedback ? (
                    <Badge tone={row.merchantFeedback === 'CONFIRMED_FRAUD' ? 'danger' : 'success'}>
                      {row.merchantFeedback.replace(/_/g, ' ')}
                    </Badge>
                  ) : (
                    <span className="text-slate-400">—</span>
                  ),
              },
              { key: 'created', header: 'Created', render: (row) => formatDateTime(row.createdAt) },
            ]}
          />
        )}
      </Card>
    </div>
  )
}
