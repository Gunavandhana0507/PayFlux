import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiErrorMessage, fraudApi } from '../api/client'
import type { FraudAnalysis } from '../api/types'
import { Badge, RiskBadge } from '../components/ui/Badge'
import { Card } from '../components/ui/Card'
import { EmptyState, ErrorState, LoadingSpinner } from '../components/ui/Feedback'
import { Table } from '../components/ui/Table'
import { formatDateTime } from '../lib/format'

export default function FraudAlerts() {
  const navigate = useNavigate()
  const [rows, setRows] = useState<FraudAnalysis[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    fraudApi
      .alerts({ size: 50 })
      .then((page) => setRows(page.items))
      .catch((err) => setError(apiErrorMessage(err, 'Could not load fraud alerts')))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-slate-800">Fraud alerts</h1>
        <p className="text-sm text-slate-500">
          Transactions the risk engine flagged as medium or high risk. Open one to review the factors and
          send feedback.
        </p>
      </div>

      {error && <ErrorState message={error} />}

      <Card bodyClassName="p-0">
        {loading ? (
          <LoadingSpinner label="Loading alerts…" />
        ) : (
          <Table
            rows={rows}
            rowKey={(row) => row.id}
            onRowClick={(row) => navigate(`/dashboard/transactions/${row.paymentId}`)}
            empty={
              <div className="p-5">
                <EmptyState
                  title="No flagged transactions"
                  description="Medium and high risk payments will be listed here."
                />
              </div>
            }
            columns={[
              { key: 'risk', header: 'Risk', render: (row) => <RiskBadge level={row.riskLevel} score={row.riskScore} /> },
              { key: 'decision', header: 'Decision', render: (row) => <Badge tone="primary">{row.decision}</Badge> },
              {
                key: 'factors',
                header: 'Top factors',
                render: (row) => (
                  <span className="text-sm text-slate-600">
                    {row.factors.slice(0, 2).map((factor) => factor.description).join('; ') || '—'}
                  </span>
                ),
              },
              {
                key: 'feedback',
                header: 'Merchant verdict',
                render: (row) =>
                  row.merchantFeedback ? (
                    <Badge tone={row.merchantFeedback === 'CONFIRMED_FRAUD' ? 'danger' : 'success'}>
                      {row.merchantFeedback.replace(/_/g, ' ')}
                    </Badge>
                  ) : (
                    <span className="text-slate-400">Pending review</span>
                  ),
              },
              { key: 'created', header: 'Detected', render: (row) => formatDateTime(row.createdAt) },
            ]}
          />
        )}
      </Card>
    </div>
  )
}
