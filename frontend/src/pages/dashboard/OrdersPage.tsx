import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ordersApi } from '../../api/orders'
import { useAsync } from '../../hooks/useAsync'
import { formatDateTime, formatMoney } from '../../lib/format'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { EmptyState } from '../../components/ui/EmptyState'
import { Input, Select, Textarea } from '../../components/ui/Input'
import { LoadingSpinner } from '../../components/ui/LoadingSpinner'
import { Modal } from '../../components/ui/Modal'
import { PageHeader } from '../../components/ui/PageHeader'
import { Pagination } from '../../components/ui/Pagination'
import { StatusBadge } from '../../components/ui/Badge'
import { Table } from '../../components/ui/Table'
import { getFriendlyError } from '../../lib/errors'
import type { OrderDto, OrderStatus } from '../../types/api'
import { useToast } from '../../components/ui/Toast'

export function OrdersPage() {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<OrderStatus | ''>('')
  const [open, setOpen] = useState(false)
  const [created, setCreated] = useState<OrderDto | null>(null)
  const [form, setForm] = useState({
    amount: '',
    notes: '',
    customerEmail: '',
    expiresInMinutes: '15',
  })
  const [formError, setFormError] = useState('')
  const toast = useToast()
  const result = useAsync(
    () =>
      ordersApi
        .list({ page, size: 10, status: status || undefined })
        .then((response) => response.data),
    [page, status],
  )
  const create = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!Number(form.amount) || Number(form.amount) <= 0) {
      setFormError('Enter an amount greater than zero')
      return
    }
    try {
      const response = await ordersApi.create(
        {
          amount: Number(form.amount),
          notes: form.notes || undefined,
          customerEmail: form.customerEmail || undefined,
          expiresInMinutes: Number(form.expiresInMinutes),
        },
        crypto.randomUUID(),
      )
      setCreated(response.data)
      setOpen(false)
      toast.show('Order created', 'success')
      void result.reload()
    } catch (error) {
      setFormError(getFriendlyError(error).message)
    }
  }
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
  const data = result.data
  return (
    <>
      <PageHeader
        title="Orders"
        subtitle="Create and manage payment links."
        actions={
          <Button
            onClick={() => {
              setOpen(true)
              setCreated(null)
            }}
          >
            Create order
          </Button>
        }
      />
      <Card>
        <div className="mb-5 flex max-w-xs items-end gap-3">
          <Select
            label="Status"
            value={status}
            onChange={(e) => {
              setPage(0)
              setStatus(e.target.value as OrderStatus | '')
            }}
          >
            <option value="">All</option>
            <option value="CREATED">Awaiting payment</option>
            <option value="PAID">Paid</option>
            <option value="EXPIRED">Expired</option>
          </Select>
        </div>
        {data?.items.length ? (
          <Table
            rows={data.items}
            columns={[
              {
                key: 'id',
                header: 'Order ID',
                render: (row) => (
                  <Link
                    className="font-medium text-primary-dark hover:underline"
                    to={`/dashboard/orders/${row.id}`}
                  >
                    {row.id}
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
              { key: 'created', header: 'Created', render: (row) => formatDateTime(row.createdAt) },
              { key: 'expires', header: 'Expires', render: (row) => formatDateTime(row.expiresAt) },
            ]}
          />
        ) : (
          <EmptyState
            title="No orders yet"
            description="Create your first payment link to get started."
            action={<Button onClick={() => setOpen(true)}>Create order</Button>}
          />
        )}
        <Pagination
          page={data?.page ?? 0}
          size={data?.size ?? 10}
          total={data?.total ?? 0}
          onChange={setPage}
        />
      </Card>
      <Modal open={open} title="Create order" onClose={() => setOpen(false)}>
        <form className="space-y-4" onSubmit={create}>
          <Input
            label="Amount"
            type="number"
            min="0.01"
            step="0.01"
            required
            value={form.amount}
            onChange={(e) => setForm({ ...form, amount: e.target.value })}
          />
          <Textarea
            label="Notes"
            value={form.notes}
            onChange={(e) => setForm({ ...form, notes: e.target.value })}
          />
          <Input
            label="Customer email (optional)"
            type="email"
            value={form.customerEmail}
            onChange={(e) => setForm({ ...form, customerEmail: e.target.value })}
          />
          <Input
            label="Expires in minutes"
            type="number"
            min="1"
            max="1440"
            value={form.expiresInMinutes}
            onChange={(e) => setForm({ ...form, expiresInMinutes: e.target.value })}
          />
          {formError && <p className="text-sm text-rose-600">{formError}</p>}
          <Button type="submit" className="w-full">
            Create order
          </Button>
        </form>
      </Modal>
      {created && (
        <Modal open={Boolean(created)} title="Order created" onClose={() => setCreated(null)}>
          <p className="text-sm text-slate-600">Share this payment link with your customer.</p>
          <div className="mt-4 rounded-md bg-slate-50 p-3 text-sm break-all">
            {created.paymentUrl}
          </div>
          <div className="mt-5 flex gap-2">
            <Button
              onClick={() => {
                void navigator.clipboard.writeText(created.paymentUrl)
                toast.show('Payment link copied', 'success')
              }}
            >
              Copy
            </Button>
            <a href={created.paymentUrl} target="_blank" rel="noreferrer">
              <Button variant="secondary">Open payment page</Button>
            </a>
          </div>
        </Modal>
      )}
    </>
  )
}
