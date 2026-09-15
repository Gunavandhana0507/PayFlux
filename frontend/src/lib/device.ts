export function getDeviceId() {
  const key = 'payflux_device'
  const current = localStorage.getItem(key)
  if (current) return current
  const value = crypto.randomUUID()
  localStorage.setItem(key, value)
  return value
}
