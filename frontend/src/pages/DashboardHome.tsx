import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { apiErrorMessage, dashboardApi, paymentApi } from '../api/client'
import type { DashboardSummary, Payment } from '../api/types'
import { RiskBadge, StatusBadge } from '../components/ui/Badge'
import { Card, StatCard } from '../components/ui/Card'
import { EmptyState, ErrorState, LoadingSpinner } from '../components/ui/Feedback'
import { Table } from '../components/ui/Table'
import { formatDateTime, formatMoney } from '../lib/format'

export default function DashboardHome() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [recent, setRecent] = useState<Payment[]>([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([dashboardApi.summary(), paymentApi.list({ page: 0, size: 5 })])
      .then(([summaryData, payments]) => {
        setSummary(summaryData)
        setRecent(payments.items)
      })
      .catch((err) => setError(apiErrorMessage(err, 'Could not load the dashboard')))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <LoadingSpinner label="Loading dashboard…" />
  if (error) return <ErrorState message={error} />
  if (!summary) return null

  const chartData = summary.series.map((point) => ({
    date: point.date.slice(5),
    volume: Number(point.volume),
    count: point.count,
  }))

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-slate-800">Overview</h1>
        <p className="text-sm text-slate-500">Payment activity across the last 14 days.</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Captured volume" value={formatMoney(summary.capturedVolume)} />
        <StatCard label="Refunded volume" value={formatMoney(summary.refundedVolume)} />
        <StatCard
          label="Successful payments"
          value={summary.successfulPayments}
          sub={`${summary.failedPayments} failed or rejected`}
        />
        <StatCard
          label="Flagged transactions"
          value={summary.flaggedTransactions}
          sub="Medium or high risk"
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card title="Captured volume" description="Daily captured amount">
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chartData}>
                <CartesianGrid stroke="#E2E8F0" strokeDasharray="3 3" />
                <XAxis dataKey="date" stroke="#94A3B8" fontSize={12} />
                <YAxis stroke="#94A3B8" fontSize={12} />
                <Tooltip formatter={(value) => formatMoney(Number(value))} />
                <Line type="monotone" dataKey="volume" stroke="#5C7C99" strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card title="Payment attempts" description="Daily attempt count">
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData}>
                <CartesianGrid stroke="#E2E8F0" strokeDasharray="3 3" />
                <XAxis dataKey="date" stroke="#94A3B8" fontSize={12} />
                <YAxis stroke="#94A3B8" fontSize={12} allowDecimals={false} />
                <Tooltip />
                <Bar dataKey="count" fill="#7A97AE" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </div>

      <Card
        title="Recent transactions"
        actions={
          <Link className="text-sm font-medium text-primary hover:text-primary-dark" to="/dashboard/transactions">
            View all
          </Link>
        }
        bodyClassName="p-0"
      >
        <Table
          rows={recent}
          rowKey={(row) => row.id}
          empty={
            <div className="p-5">
              <EmptyState
                title="No transactions yet"
                description="Create an order and complete a payment to see activity here."
              />
            </div>
          }
          columns={[
            { key: 'id', header: 'Transaction', render: (row) => <span className="font-mono text-xs">{row.id.slice(0, 8)}</span> },
            { key: 'amount', header: 'Amount', render: (row) => formatMoney(row.amount, row.currency) },
            { key: 'method', header: 'Method', render: (row) => row.method.replace('_', ' ') },
            { key: 'status', header: 'Status', render: (row) => <StatusBadge status={row.status} /> },
            { key: 'risk', header: 'Risk', render: (row) => <RiskBadge level={row.riskLevel} score={row.riskScore} /> },
            { key: 'created', header: 'Created', render: (row) => formatDateTime(row.createdAt) },
          ]}
        />
      </Card>
    </div>
  )
}
