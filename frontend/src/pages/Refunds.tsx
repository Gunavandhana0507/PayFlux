import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiErrorMessage, refundApi } from '../api/client'
import type { Refund } from '../api/types'
import { StatusBadge } from '../components/ui/Badge'
import { Card } from '../components/ui/Card'
import { EmptyState, ErrorState, LoadingSpinner } from '../components/ui/Feedback'
import { Table } from '../components/ui/Table'
import { formatDateTime, formatMoney } from '../lib/format'

export default function Refunds() {
  const navigate = useNavigate()
  const [rows, setRows] = useState<Refund[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    refundApi
      .list({ size: 50 })
      .then((page) => setRows(page.items))
      .catch((err) => setError(apiErrorMessage(err, 'Could not load refunds')))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-slate-800">Refunds</h1>
        <p className="text-sm text-slate-500">Full and partial refunds against captured payments.</p>
      </div>

      {error && <ErrorState message={error} />}

      <Card bodyClassName="p-0">
        {loading ? (
          <LoadingSpinner label="Loading refunds…" />
        ) : (
          <Table
            rows={rows}
            rowKey={(row) => row.id}
            onRowClick={(row) => navigate(`/dashboard/transactions/${row.paymentId}`)}
            empty={
              <div className="p-5">
                <EmptyState
                  title="No refunds yet"
                  description="Open a captured transaction to issue a full or partial refund."
                />
              </div>
            }
            columns={[
              { key: 'id', header: 'Refund', render: (row) => <span className="font-mono text-xs">{row.id.slice(0, 8)}</span> },
              { key: 'payment', header: 'Transaction', render: (row) => <span className="font-mono text-xs">{row.paymentId.slice(0, 8)}</span> },
              { key: 'amount', header: 'Amount', render: (row) => formatMoney(row.amount, row.currency) },
              { key: 'status', header: 'Status', render: (row) => <StatusBadge status={row.status} /> },
              { key: 'reason', header: 'Reason', render: (row) => row.reason ?? '—' },
              { key: 'created', header: 'Created', render: (row) => formatDateTime(row.createdAt) },
              { key: 'processed', header: 'Processed', render: (row) => formatDateTime(row.processedAt) },
            ]}
          />
        )}
      </Card>
    </div>
  )
}
