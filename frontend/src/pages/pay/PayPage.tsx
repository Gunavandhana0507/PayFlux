import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { publicPayApi } from '../../api/publicPay'
import { useAsync } from '../../hooks/useAsync'
import { getDeviceId } from '../../lib/device'
import { countdown, formatMoney } from '../../lib/format'
import { getFriendlyError } from '../../lib/errors'
import { Button } from '../../components/ui/Button'
import { Input, Select } from '../../components/ui/Input'
import { LoadingSpinner } from '../../components/ui/LoadingSpinner'
import { PaymentMethodPicker } from '../../components/PaymentMethodPicker'
import type { PaymentMethod, PublicPaymentDto } from '../../types/api'

export function PayPage() {
  const { orderId = '' } = useParams()
  const result = useAsync(() => publicPayApi.order(orderId).then((response) => response.data), [orderId])
  const [seconds, setSeconds] = useState(0)
  const [method, setMethod] = useState<PaymentMethod>('CARD')
  const [email, setEmail] = useState(() => localStorage.getItem('payflux_email') ?? '')
  const [card, setCard] = useState({ number: '', expiry: '', holderName: '' })
  const [upiId, setUpiId] = useState('')
  const [bankCode, setBankCode] = useState('HDFC')
  const [walletProvider, setWalletProvider] = useState('Paytm')
  const [payment, setPayment] = useState<PublicPaymentDto | null>(null)
  const [otp, setOtp] = useState('')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  useEffect(() => { setSeconds(result.data?.secondsRemaining ?? 0) }, [result.data?.secondsRemaining])
  useEffect(() => { const timer = window.setInterval(() => setSeconds((value) => Math.max(0, value - 1)), 1000); return () => window.clearInterval(timer) }, [])
  if (result.loading) return <LoadingSpinner fullPage />
  if (result.error || !result.data) return <PayShell><div className="py-10 text-center"><h1 className="text-xl font-semibold">We couldn't find this payment link.</h1><p className="mt-2 text-sm text-slate-500">Check the link and try again.</p></div></PayShell>
  const order = result.data
  const current = payment || order.latestPayment
  const submit = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!email.includes('@')) { setMessage('Enter a valid email address'); return }
    if (method === 'UPI' && !upiId.includes('@')) { setMessage('Enter a valid UPI ID'); return }
    setLoading(true); setMessage('')
    try {
      const payload = { method, customerEmail: email, deviceId: getDeviceId(), ...(method === 'CARD' ? { card: { number: card.number.replace(/\s/g, ''), expiryMonth: Number(card.expiry.split('/')[0]), expiryYear: 2000 + Number(card.expiry.split('/')[1]), holderName: card.holderName } } : {}), ...(method === 'UPI' ? { upiId } : {}), ...(method === 'NETBANKING' ? { bankCode } : {}), ...(method === 'WALLET' ? { walletProvider } : {}) }
      const response = await publicPayApi.pay(orderId, payload, crypto.randomUUID()); setPayment(response.data); localStorage.setItem('payflux_email', email)
    } catch (error) { setMessage(getFriendlyError(error).message) } finally { setLoading(false) }
  }
  const verify = async (event: React.FormEvent) => { event.preventDefault(); setLoading(true); try { const response = await publicPayApi.verify(payment!.id, otp); setPayment(response.data) } catch (error) { setMessage(getFriendlyError(error).message) } finally { setLoading(false) } }
  if (current?.status === 'CAPTURED') return <PayShell><Result title="Payment successful" payment={current} /></PayShell>
  if (current?.status === 'REJECTED') return <PayShell><Result title="We couldn't complete this payment" payment={current} /><p className="mt-3 text-center text-sm text-slate-600">Please contact the merchant if you need help.</p></PayShell>
  if (current?.status === 'FAILED') return <PayShell><Result title="Payment failed" payment={current} /><Button className="mt-5 w-full" onClick={() => setPayment(null)}>Try again</Button></PayShell>
  if (current?.verificationRequired || current?.status === 'VERIFICATION_REQUIRED') return <PayShell><h1 className="text-xl font-semibold">One more step — enter the 6-digit code sent to your email</h1><p className="mt-2 text-sm text-slate-500">Test mode: use 123456</p><form onSubmit={verify} className="mt-6 space-y-4"><Input label="Verification code" value={otp} maxLength={6} required onChange={(e) => setOtp(e.target.value)} />{message && <p className="text-sm text-rose-600">{message}</p>}<Button className="w-full" loading={loading}>Verify payment</Button></form></PayShell>
  return <PayShell><div className="mb-6 flex justify-between text-xs font-medium text-primary-dark"><span>Review</span><span>Pay</span><span>Done</span></div><h1 className="text-xl font-semibold text-slate-900">Pay {formatMoney(order.amount, order.currency)}</h1><p className="mt-1 text-sm text-slate-500">To {order.merchantName}</p>{order.notes && <p className="mt-4 rounded-md bg-slate-50 p-3 text-sm text-slate-600">{order.notes}</p>}<p className={`mt-4 text-sm ${seconds < 120 ? 'text-amber-700' : 'text-slate-500'}`}>Payment link expires in {countdown(seconds)}</p>{order.payableNow ? <form onSubmit={submit} className="mt-6 space-y-5"><Input label="Email" type="email" value={email} required onChange={(e) => setEmail(e.target.value)} /><PaymentMethodPicker value={method} onChange={setMethod} />{method === 'CARD' && <div className="space-y-4"><Input label="Card number" inputMode="numeric" value={card.number} onChange={(e) => setCard({ ...card, number: e.target.value.replace(/\D/g, '').replace(/(.{4})/g, '$1 ').trim() })} required /><div className="grid grid-cols-2 gap-3"><Input label="Expiry MM/YY" value={card.expiry} onChange={(e) => setCard({ ...card, expiry: e.target.value })} required /><Input label="Name on card" value={card.holderName} onChange={(e) => setCard({ ...card, holderName: e.target.value })} required /></div></div>}{method === 'UPI' && <Input label="UPI ID" value={upiId} onChange={(e) => setUpiId(e.target.value)} placeholder="name@bank" required />}{method === 'NETBANKING' && <Select label="Bank" value={bankCode} onChange={(e) => setBankCode(e.target.value)}>{[['HDFC', 'HDFC'], ['ICICI', 'ICICI'], ['SBI', 'SBI'], ['AXIS', 'Axis'], ['KOTAK', 'Kotak']].map(([value, label]) => <option key={value} value={value}>{label}</option>)}</Select>}{method === 'WALLET' && <Select label="Wallet" value={walletProvider} onChange={(e) => setWalletProvider(e.target.value)}>{['Paytm', 'PhonePe', 'Amazon Pay'].map((value) => <option key={value}>{value}</option>)}</Select>}{message && <p className="text-sm text-rose-600">{message}</p>}<p className="text-xs text-slate-500">Test mode — no real money moves.</p><Button className="w-full" loading={loading}>Pay {formatMoney(order.amount, order.currency)}</Button></form> : <p className="mt-6 rounded-md bg-amber-50 p-4 text-sm text-amber-800">This payment link is not available for payment.</p>}</PayShell>
}

function PayShell({ children }: { children: React.ReactNode }) { return <div className="mx-auto max-w-md px-5 py-8"><div className="mb-6 text-center text-xl font-bold text-primary-dark">PayFlux</div><div className="card">{children}</div></div> }
function Result({ title, payment }: { title: string; payment: PublicPaymentDto }) { return <div className="py-8 text-center"><h1 className="text-xl font-semibold text-slate-900">{title}</h1><p className="mt-3 text-sm text-slate-600">{payment.failureReason || `Payment ID: ${payment.id}`}</p>{payment.status === 'CAPTURED' && <p className="mt-2 text-sm text-slate-500">You can close this page.</p>}</div> }
