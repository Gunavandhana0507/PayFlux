import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiErrorMessage, newIdempotencyKey, orderApi } from '../api/client'
import type { Order } from '../api/types'
import { StatusBadge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { EmptyState, ErrorState, LoadingSpinner } from '../components/ui/Feedback'
import { Input, Select, Textarea } from '../components/ui/Input'
import { Modal } from '../components/ui/Modal'
import { Table } from '../components/ui/Table'
import { useToast } from '../components/ui/Toast'
import { formatDateTime, formatMoney } from '../lib/format'

const statuses = ['CREATED', 'ATTEMPTED', 'PAID', 'EXPIRED', 'CANCELLED']

export default function Orders() {
  const navigate = useNavigate()
  const { notify } = useToast()
  const [orders, setOrders] = useState<Order[]>([])
  const [status, setStatus] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [open, setOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState({
    amount: '',
    currency: 'INR',
    receipt: '',
    description: '',
    notes: '',
    expiryMinutes: '',
    customerName: '',
    customerEmail: '',
    customerPhone: '',
  })

  const load = useCallback(() => {
    setLoading(true)
    orderApi
      .list({ status: status || undefined, size: 50 })
      .then((page) => setOrders(page.items))
      .catch((err) => setError(apiErrorMessage(err, 'Could not load orders')))
      .finally(() => setLoading(false))
  }, [status])

  useEffect(load, [load])

  async function createOrder(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    try {
      const order = await orderApi.create(
        {
          amount: Number(form.amount),
          currency: form.currency,
          receipt: form.receipt || undefined,
          description: form.description || undefined,
          notes: form.notes || undefined,
          expiryMinutes: form.expiryMinutes ? Number(form.expiryMinutes) : undefined,
          customerName: form.customerName || undefined,
          customerEmail: form.customerEmail || undefined,
          customerPhone: form.customerPhone || undefined,
        },
        newIdempotencyKey(),
      )
      notify('Order created', 'success')
      setOpen(false)
      navigate(`/dashboard/orders/${order.id}`)
    } catch (err) {
      notify(apiErrorMessage(err, 'Could not create the order'), 'error')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold text-slate-800">Orders</h1>
          <p className="text-sm text-slate-500">Payment intents shared with your customers.</p>
        </div>
        <div className="flex items-end gap-3">
          <Select
            label="Status"
            value={status}
            className="w-44"
            onChange={(event) => setStatus(event.target.value)}
          >
            <option value="">All statuses</option>
            {statuses.map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </Select>
          <Button onClick={() => setOpen(true)}>Create order</Button>
        </div>
      </div>

      {error && <ErrorState message={error} />}

      <Card bodyClassName="p-0">
        {loading ? (
          <LoadingSpinner label="Loading orders…" />
        ) : (
          <Table
            rows={orders}
            rowKey={(row) => row.id}
            onRowClick={(row) => navigate(`/dashboard/orders/${row.id}`)}
            empty={
              <div className="p-5">
                <EmptyState
                  title="No orders yet"
                  description="Create your first order to generate a hosted checkout link."
                  action={<Button onClick={() => setOpen(true)}>Create order</Button>}
                />
              </div>
            }
            columns={[
              {
                key: 'id',
                header: 'Order',
                render: (row) => (
                  <div>
                    <span className="font-mono text-xs text-slate-500">{row.id.slice(0, 8)}</span>
                    <p className="text-sm text-slate-700">{row.receipt ?? row.description ?? '—'}</p>
                  </div>
                ),
              },
              { key: 'amount', header: 'Amount', render: (row) => formatMoney(row.amount, row.currency) },
              { key: 'customer', header: 'Customer', render: (row) => row.customerName ?? row.customerEmail ?? '—' },
              { key: 'status', header: 'Status', render: (row) => <StatusBadge status={row.status} /> },
              { key: 'created', header: 'Created', render: (row) => formatDateTime(row.createdAt) },
              { key: 'expires', header: 'Expires', render: (row) => formatDateTime(row.expiresAt) },
            ]}
          />
        )}
      </Card>

      <Modal
        open={open}
        title="Create order"
        description="Orders expire 15 minutes after creation unless you override it."
        onClose={() => setOpen(false)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" form="create-order" loading={saving}>
              Create order
            </Button>
          </>
        }
      >
        <form id="create-order" className="grid gap-4 sm:grid-cols-2" onSubmit={createOrder}>
          <Input
            label="Amount"
            type="number"
            min="1"
            step="0.01"
            required
            value={form.amount}
            onChange={(event) => setForm({ ...form, amount: event.target.value })}
          />
          <Input
            label="Currency"
            required
            maxLength={3}
            value={form.currency}
            onChange={(event) => setForm({ ...form, currency: event.target.value.toUpperCase() })}
          />
          <Input
            label="Receipt"
            value={form.receipt}
            onChange={(event) => setForm({ ...form, receipt: event.target.value })}
          />
          <Input
            label="Expiry (minutes)"
            type="number"
            min="1"
            placeholder="15"
            value={form.expiryMinutes}
            onChange={(event) => setForm({ ...form, expiryMinutes: event.target.value })}
          />
          <Input
            label="Customer name"
            value={form.customerName}
            onChange={(event) => setForm({ ...form, customerName: event.target.value })}
          />
          <Input
            label="Customer email"
            type="email"
            value={form.customerEmail}
            onChange={(event) => setForm({ ...form, customerEmail: event.target.value })}
          />
          <Input
            label="Customer phone"
            value={form.customerPhone}
            onChange={(event) => setForm({ ...form, customerPhone: event.target.value })}
          />
          <Input
            label="Description"
            value={form.description}
            onChange={(event) => setForm({ ...form, description: event.target.value })}
          />
          <div className="sm:col-span-2">
            <Textarea
              label="Internal notes"
              rows={2}
              value={form.notes}
              onChange={(event) => setForm({ ...form, notes: event.target.value })}
            />
          </div>
        </form>
      </Modal>
    </div>
  )
}
