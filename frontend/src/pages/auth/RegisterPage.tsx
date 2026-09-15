import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Button } from '../../components/ui/Button'
import { Input, Select } from '../../components/ui/Input'
import { getFriendlyError } from '../../lib/errors'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../components/ui/Toast'

const initial = { businessName: '', businessType: '', gstId: '', email: '', password: '' }
export function RegisterPage() {
  const { register } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [form, setForm] = useState(initial)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const submit = async (event: React.FormEvent) => {
    event.preventDefault()
    const next: Record<string, string> = {}
    if (!form.businessName.trim()) next.businessName = 'Business name is required'
    if (!form.businessType) next.businessType = 'Select a business type'
    if (form.gstId.length !== 15) next.gstId = 'GST ID must be 15 characters'
    if (!form.email.includes('@')) next.email = 'Enter a valid email address'
    if (form.password.length < 8) next.password = 'Use at least 8 characters'
    setErrors(next)
    if (Object.keys(next).length) return
    setLoading(true)
    try {
      await register(form)
      toast.show('Welcome to PayFlux', 'success')
      navigate('/dashboard', { replace: true })
    } catch (error) {
      const friendly = getFriendlyError(error)
      setMessage(friendly.message)
      setErrors(friendly.fieldErrors)
    } finally {
      setLoading(false)
    }
  }
  const update = (key: keyof typeof form, value: string) => setForm({ ...form, [key]: value })
  return (
    <div className="mx-auto max-w-lg px-5 py-10">
      <div className="card">
        <h1 className="text-2xl font-semibold text-slate-900">Create your PayFlux account</h1>
        <p className="mt-1 text-sm text-slate-500">Start accepting payments in minutes.</p>
        {message && (
          <div className="mt-5 rounded-md bg-rose-50 p-3 text-sm text-rose-700">{message}</div>
        )}
        <form onSubmit={submit} className="mt-6 space-y-4">
          <Input
            label="Business name"
            value={form.businessName}
            error={errors.businessName}
            required
            onChange={(e) => update('businessName', e.target.value)}
          />
          <Select
            label="Business type"
            value={form.businessType}
            error={errors.businessType}
            required
            onChange={(e) => update('businessType', e.target.value)}
          >
            <option value="">Select one</option>
            {[
              'Retail',
              'E-commerce',
              'Services',
              'Education',
              'Travel',
              'Food & Beverage',
              'Other',
            ].map((option) => (
              <option key={option}>{option}</option>
            ))}
          </Select>
          <Input
            label="GST ID"
            value={form.gstId}
            hint="15 characters, e.g. 27AAPFU0939F1ZV"
            error={errors.gstId}
            required
            onChange={(e) => update('gstId', e.target.value.toUpperCase())}
          />
          <Input
            label="Email"
            type="email"
            value={form.email}
            error={errors.email}
            required
            onChange={(e) => update('email', e.target.value)}
          />
          <Input
            label="Password"
            type="password"
            value={form.password}
            error={errors.password}
            hint="At least 8 characters"
            required
            onChange={(e) => update('password', e.target.value)}
          />
          <Button type="submit" loading={loading} className="w-full">
            Create account
          </Button>
        </form>
        <p className="mt-6 text-center text-sm text-slate-500">
          Already registered?{' '}
          <Link to="/login" className="font-medium text-primary-dark hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  )
}
