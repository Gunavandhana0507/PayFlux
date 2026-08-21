import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import { apiErrorMessage, checkoutApi, newIdempotencyKey } from '../api/client'
import type { InitiatePaymentPayload, PaymentMethod, PublicOrder, PublicPayment } from '../api/types'
import { Logo } from '../components/AppLayout'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { EmptyState, ErrorState, LoadingSpinner } from '../components/ui/Feedback'
import { Input, Select } from '../components/ui/Input'
import { deviceFingerprint, formatDateTime, formatMoney } from '../lib/format'

const methods: { value: PaymentMethod; label: string; hint: string }[] = [
  { value: 'CARD', label: 'Card', hint: 'Credit or debit card' },
  { value: 'UPI', label: 'UPI', hint: 'Pay with a UPI ID' },
  { value: 'NET_BANKING', label: 'Net Banking', hint: 'Your bank account' },
  { value: 'WALLET', label: 'Wallet', hint: 'Prepaid wallet balance' },
]

const banks = ['HDFC', 'ICICI', 'SBI', 'AXIS', 'KOTAK']
const wallets = ['PAYTM', 'PHONEPE', 'AMAZONPAY', 'MOBIKWIK']

export default function Checkout() {
  const { orderId = '' } = useParams()
  const [order, setOrder] = useState<PublicOrder | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const [method, setMethod] = useState<PaymentMethod>('CARD')
  const [form, setForm] = useState({
    cardNumber: '',
    cardHolderName: '',
    cardExpiry: '',
    cardCvv: '',
    upiVpa: '',
    bankCode: 'HDFC',
    walletProvider: 'PAYTM',
    simulateOutcome: 'SUCCESS' as 'SUCCESS' | 'FAILURE' | 'TIMEOUT',
  })
  const [submitting, setSubmitting] = useState(false)
  const [payment, setPayment] = useState<PublicPayment | null>(null)
  const [payError, setPayError] = useState('')
  const [otp, setOtp] = useState('')
  const [verifying, setVerifying] = useState(false)
  const [idempotencyKey, setIdempotencyKey] = useState(newIdempotencyKey)

  useEffect(() => {
    checkoutApi
      .order(orderId)
      .then(setOrder)
      .catch((err) => setLoadError(apiErrorMessage(err, 'This payment link is not valid')))
      .finally(() => setLoading(false))
  }, [orderId])

  const needsOtp = payment?.status === 'VERIFICATION_REQUIRED'
  const succeeded = payment?.status === 'CAPTURED'
  const failed = payment ? ['FAILED', 'REJECTED'].includes(payment.status) : false

  const payload: InitiatePaymentPayload = useMemo(
    () => ({
      orderId,
      method,
      deviceFingerprint: deviceFingerprint(),
      simulateOutcome: form.simulateOutcome,
      ...(method === 'CARD'
        ? {
            cardNumber: form.cardNumber.replace(/\s+/g, ''),
            cardHolderName: form.cardHolderName,
            cardExpiry: form.cardExpiry,
            cardCvv: form.cardCvv,
          }
        : {}),
      ...(method === 'UPI' ? { upiVpa: form.upiVpa } : {}),
      ...(method === 'NET_BANKING' ? { bankCode: form.bankCode } : {}),
      ...(method === 'WALLET' ? { walletProvider: form.walletProvider } : {}),
    }),
    [form, method, orderId],
  )

  async function pay(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setPayError('')
    try {
      setPayment(await checkoutApi.pay(payload, idempotencyKey))
    } catch (err) {
      setPayError(apiErrorMessage(err, 'The payment could not be processed'))
    } finally {
      setSubmitting(false)
    }
  }

  async function verify(event: FormEvent) {
    event.preventDefault()
    if (!payment) return
    setVerifying(true)
    setPayError('')
    try {
      setPayment(await checkoutApi.verify(payment.id, otp))
      setOtp('')
    } catch (err) {
      setPayError(apiErrorMessage(err, 'Verification failed'))
    } finally {
      setVerifying(false)
    }
  }

  function retry() {
    setPayment(null)
    setPayError('')
    setIdempotencyKey(newIdempotencyKey())
  }

  if (loading) return <LoadingSpinner label="Loading payment details…" />

  if (loadError) {
    return (
      <div className="mx-auto max-w-lg px-4 py-16">
        <ErrorState message={loadError} />
      </div>
    )
  }
  if (!order) return null

  return (
    <div className="min-h-full bg-slate-50 px-4 py-10">
      <div className="mx-auto grid w-full max-w-4xl gap-6 lg:grid-cols-[380px,1fr]">
        <div className="space-y-4">
          <Logo subtitle="Secure checkout" />
          <Card title={order.merchantName} description={order.description ?? 'Payment request'}>
            <p className="text-3xl font-semibold text-slate-800">
              {formatMoney(order.amount, order.currency)}
            </p>
            <dl className="mt-4 space-y-2 text-sm">
              <div className="flex justify-between">
                <dt className="text-slate-500">Order</dt>
                <dd className="font-mono text-xs text-slate-600">{order.id.slice(0, 12)}</dd>
              </div>
              {order.customerName && (
                <div className="flex justify-between">
                  <dt className="text-slate-500">Customer</dt>
                  <dd className="text-slate-700">{order.customerName}</dd>
                </div>
              )}
              {order.customerEmail && (
                <div className="flex justify-between">
                  <dt className="text-slate-500">Email</dt>
                  <dd className="text-slate-700">{order.customerEmail}</dd>
                </div>
              )}
              <div className="flex justify-between">
                <dt className="text-slate-500">Expires</dt>
                <dd className="text-slate-700">{formatDateTime(order.expiresAt)}</dd>
              </div>
            </dl>
          </Card>
        </div>

        <div className="space-y-4">
          {!order.payable && !payment && (
            <Card>
              <EmptyState
                title={`This order is ${order.status.toLowerCase()}`}
                description="Ask the merchant for a fresh payment link to continue."
              />
            </Card>
          )}

          {succeeded && (
            <Card title="Payment successful">
              <p className="text-sm text-slate-600">
                {formatMoney(payment?.amount, payment?.currency)} was captured. A receipt has been sent to the
                merchant.
              </p>
              <p className="mt-2 font-mono text-xs text-slate-400">{payment?.id}</p>
            </Card>
          )}

          {failed && (
            <Card title="Payment unsuccessful">
              <p className="text-sm text-slate-600">
                {payment?.failureReason ?? payment?.message ?? 'The payment did not go through.'}
              </p>
              <Button className="mt-4" variant="secondary" onClick={retry}>
                Try another method
              </Button>
            </Card>
          )}

          {needsOtp && (
            <Card title="Verify it's you" description={payment?.message}>
              <form className="space-y-4" onSubmit={verify}>
                {payError && <ErrorState message={payError} />}
                <Input
                  label="One-time password"
                  inputMode="numeric"
                  required
                  value={otp}
                  onChange={(event) => setOtp(event.target.value)}
                />
                <Button type="submit" className="w-full" loading={verifying}>
                  Verify and pay
                </Button>
              </form>
            </Card>
          )}

          {order.payable && !payment && (
            <Card title="Choose a payment method">
              <form className="space-y-5" onSubmit={pay}>
                {payError && <ErrorState message={payError} />}

                <div className="grid gap-3 sm:grid-cols-2">
                  {methods.map((option) => (
                    <button
                      key={option.value}
                      type="button"
                      onClick={() => setMethod(option.value)}
                      className={
                        method === option.value
                          ? 'rounded-lg border-2 border-primary bg-primary-light/20 px-4 py-3 text-left'
                          : 'rounded-lg border border-slate-200 bg-white px-4 py-3 text-left hover:border-primary-light'
                      }
                    >
                      <span className="block text-sm font-medium text-slate-800">{option.label}</span>
                      <span className="block text-xs text-slate-500">{option.hint}</span>
                    </button>
                  ))}
                </div>

                {method === 'CARD' && (
                  <div className="grid gap-4 sm:grid-cols-2">
                    <div className="sm:col-span-2">
                      <Input
                        label="Card number"
                        required
                        inputMode="numeric"
                        placeholder="4111 1111 1111 1111"
                        value={form.cardNumber}
                        onChange={(event) => setForm({ ...form, cardNumber: event.target.value })}
                      />
                    </div>
                    <div className="sm:col-span-2">
                      <Input
                        label="Name on card"
                        required
                        value={form.cardHolderName}
                        onChange={(event) => setForm({ ...form, cardHolderName: event.target.value })}
                      />
                    </div>
                    <Input
                      label="Expiry"
                      required
                      placeholder="MM/YY"
                      value={form.cardExpiry}
                      onChange={(event) => setForm({ ...form, cardExpiry: event.target.value })}
                    />
                    <Input
                      label="CVV"
                      required
                      type="password"
                      maxLength={4}
                      value={form.cardCvv}
                      onChange={(event) => setForm({ ...form, cardCvv: event.target.value })}
                    />
                  </div>
                )}

                {method === 'UPI' && (
                  <Input
                    label="UPI ID"
                    required
                    placeholder="name@bank"
                    value={form.upiVpa}
                    onChange={(event) => setForm({ ...form, upiVpa: event.target.value })}
                  />
                )}

                {method === 'NET_BANKING' && (
                  <Select
                    label="Bank"
                    value={form.bankCode}
                    onChange={(event) => setForm({ ...form, bankCode: event.target.value })}
                  >
                    {banks.map((bank) => (
                      <option key={bank} value={bank}>
                        {bank}
                      </option>
                    ))}
                  </Select>
                )}

                {method === 'WALLET' && (
                  <Select
                    label="Wallet"
                    value={form.walletProvider}
                    onChange={(event) => setForm({ ...form, walletProvider: event.target.value })}
                  >
                    {wallets.map((wallet) => (
                      <option key={wallet} value={wallet}>
                        {wallet}
                      </option>
                    ))}
                  </Select>
                )}

                <div className="rounded-lg border border-slate-200 bg-slate-50 px-4 py-3">
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="text-sm font-medium text-slate-700">Sandbox processor</p>
                      <p className="text-xs text-slate-500">
                        Force the mock processor result while testing.
                      </p>
                    </div>
                    <Badge tone="primary">Test mode</Badge>
                  </div>
                  <div className="mt-3">
                    <Select
                      value={form.simulateOutcome}
                      onChange={(event) =>
                        setForm({
                          ...form,
                          simulateOutcome: event.target.value as 'SUCCESS' | 'FAILURE' | 'TIMEOUT',
                        })
                      }
                    >
                      <option value="SUCCESS">Success</option>
                      <option value="FAILURE">Failure</option>
                      <option value="TIMEOUT">Timeout</option>
                    </Select>
                  </div>
                </div>

                <Button type="submit" className="w-full" size="lg" loading={submitting}>
                  Pay {formatMoney(order.amount, order.currency)}
                </Button>
              </form>
            </Card>
          )}
        </div>
      </div>
    </div>
  )
}
