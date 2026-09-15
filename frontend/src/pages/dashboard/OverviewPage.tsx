import { Link } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { dashboardApi } from '../../api/dashboard'
import { useAsync } from '../../hooks/useAsync'
import { formatMoney } from '../../lib/format'
import { Card } from '../../components/ui/Card'
import { EmptyState } from '../../components/ui/EmptyState'
import { LoadingSpinner } from '../../components/ui/LoadingSpinner'
import { PageHeader } from '../../components/ui/PageHeader'
import { Button } from '../../components/ui/Button'
import { StatusBadge } from '../../components/ui/Badge'

export function OverviewPage() {
  const { data, loading, error, reload } = useAsync(() => dashboardApi.summary().then((response) => response.data), [])
  if (loading) return <LoadingSpinner />
  if (error) return <div className="card text-center"><p className="text-sm text-rose-600">{error}</p><Button className="mt-4" onClick={reload}>Try again</Button></div>
  if (!data) return null
  const stats = [['Payments', data.today.totalPayments], ['Successful', data.today.successful], ['Revenue', formatMoney(data.today.revenue)], ['Refunded', formatMoney(data.today.refunded)]]
  return <><PageHeader title="Overview" subtitle="A snapshot of your payment activity today." actions={<Link to="/dashboard/orders"><Button>Create order</Button></Link>} /><div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{stats.map(([label, value]) => <Card key={label as string}><p className="text-sm text-slate-500">{label}</p><p className="mt-2 text-2xl font-semibold text-slate-900">{value}</p></Card>)}</div><div className="mt-6 grid gap-6 xl:grid-cols-2"><Card title="Payments, last 7 days"><div className="h-64"><ResponsiveContainer width="100%" height="100%"><BarChart data={data.daily}><CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" /><XAxis dataKey="date" tick={{ fontSize: 11 }} /><YAxis allowDecimals={false} tick={{ fontSize: 11 }} /><Tooltip /><Bar dataKey="captured" fill="#5C7C99" name="Successful" /><Bar dataKey="failed" fill="#e11d48" name="Failed" /></BarChart></ResponsiveContainer></div></Card><Card title="Revenue, last 7 days"><div className="h-64"><ResponsiveContainer width="100%" height="100%"><LineChart data={data.daily}><CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" /><XAxis dataKey="date" tick={{ fontSize: 11 }} /><YAxis tick={{ fontSize: 11 }} /><Tooltip formatter={(value) => formatMoney(Number(value))} /><Line type="monotone" dataKey="revenue" stroke="#5C7C99" strokeWidth={2} name="Revenue" /></LineChart></ResponsiveContainer></div></Card></div><Card title="Recent fraud alerts" className="mt-6">{data.recentAlerts.length ? <div className="divide-y divide-slate-100">{data.recentAlerts.slice(0, 5).map((alert) => <Link to={`/dashboard/transactions/${alert.id}`} key={alert.id} className="flex items-center justify-between gap-3 py-3 hover:bg-slate-50"><div><p className="text-sm font-medium text-slate-900">{alert.id}</p><p className="text-xs text-slate-500">{alert.customerEmail}</p></div><StatusBadge status={alert.riskLevel ?? 'MEDIUM'} /></Link>)}</div> : <EmptyState title="No flagged payments yet" />}</Card></>
}
