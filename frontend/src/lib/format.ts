export function formatMoney(amount: number | string | undefined, currency = 'INR'): string {
  if (amount === undefined || amount === null) return '—'
  const value = typeof amount === 'string' ? Number(amount) : amount
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency,
    maximumFractionDigits: 2,
  }).format(value)
}

export function formatDateTime(value?: string): string {
  if (!value) return '—'
  return new Date(value).toLocaleString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function humanizeFeatureName(name: string): string {
  return name.replace(/_/g, ' ').replace(/\b\w/g, (character) => character.toUpperCase())
}

/** Stable-ish per-browser device id sent with checkout for the risk engine. */
export function deviceFingerprint(): string {
  const key = 'payflux.device'
  let value = localStorage.getItem(key)
  if (!value) {
    value = `dev_${Math.random().toString(36).slice(2, 12)}`
    localStorage.setItem(key, value)
  }
  return value
}
