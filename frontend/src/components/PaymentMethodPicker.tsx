import type { PaymentMethod } from '../types/api'

export function PaymentMethodPicker({
  value,
  onChange,
}: {
  value: PaymentMethod
  onChange: (method: PaymentMethod) => void
}) {
  const options: [PaymentMethod, string][] = [
    ['CARD', 'Card'],
    ['UPI', 'UPI'],
    ['NETBANKING', 'Net Banking'],
    ['WALLET', 'Wallet'],
  ]
  return (
    <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
      {options.map(([method, label]) => (
        <button
          type="button"
          key={method}
          onClick={() => onChange(method)}
          className={`rounded-md border px-3 py-2 text-sm ${value === method ? 'border-primary bg-primary-light/30 text-primary-dark' : 'border-slate-200 bg-white text-slate-600'}`}
        >
          {label}
        </button>
      ))}
    </div>
  )
}
