import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiErrorMessage } from '../api/client'
import type { RegisterPayload } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { Logo } from '../components/AppLayout'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { ErrorState } from '../components/ui/Feedback'
import { Input, Select } from '../components/ui/Input'

const emptyForm: RegisterPayload = {
  email: '',
  password: '',
  fullName: '',
  businessName: '',
  legalName: '',
  businessType: 'RETAIL',
  websiteUrl: '',
  contactName: '',
  contactPhone: '',
  panNumber: '',
  gstin: '',
  addressLine1: '',
  addressLine2: '',
  city: '',
  state: '',
  postalCode: '',
  country: 'India',
  bankAccountName: '',
  bankAccountNumber: '',
  bankIfsc: '',
}

export default function Signup() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState<RegisterPayload>(emptyForm)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  function update<K extends keyof RegisterPayload>(key: K, value: RegisterPayload[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      await register(form)
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError(apiErrorMessage(err, 'Could not create the merchant account'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-full bg-slate-50 px-4 py-12">
      <div className="mx-auto w-full max-w-3xl">
        <div className="mb-6 flex justify-center">
          <Logo subtitle="Merchant onboarding" />
        </div>
        <form onSubmit={onSubmit} className="space-y-6">
          {error && <ErrorState message={error} />}

          <Card title="Account" description="Credentials used to sign in to the dashboard.">
            <div className="grid gap-4 sm:grid-cols-2">
              <Input
                label="Work email"
                type="email"
                required
                value={form.email}
                onChange={(event) => update('email', event.target.value)}
              />
              <Input
                label="Password"
                type="password"
                required
                minLength={8}
                hint="At least 8 characters"
                value={form.password}
                onChange={(event) => update('password', event.target.value)}
              />
              <Input
                label="Your full name"
                required
                value={form.fullName}
                onChange={(event) => update('fullName', event.target.value)}
              />
            </div>
          </Card>

          <Card title="Business & KYC" description="Details verified before your account goes live.">
            <div className="grid gap-4 sm:grid-cols-2">
              <Input
                label="Business name"
                required
                value={form.businessName}
                onChange={(event) => update('businessName', event.target.value)}
              />
              <Input
                label="Registered legal name"
                value={form.legalName}
                onChange={(event) => update('legalName', event.target.value)}
              />
              <Select
                label="Business type"
                value={form.businessType}
                onChange={(event) => update('businessType', event.target.value)}
              >
                <option value="RETAIL">Retail</option>
                <option value="ECOMMERCE">E-commerce</option>
                <option value="SERVICES">Services</option>
                <option value="SAAS">SaaS</option>
                <option value="EDUCATION">Education</option>
                <option value="OTHER">Other</option>
              </Select>
              <Input
                label="Website"
                value={form.websiteUrl}
                placeholder="https://"
                onChange={(event) => update('websiteUrl', event.target.value)}
              />
              <Input
                label="PAN"
                value={form.panNumber}
                onChange={(event) => update('panNumber', event.target.value.toUpperCase())}
              />
              <Input
                label="GSTIN"
                value={form.gstin}
                onChange={(event) => update('gstin', event.target.value.toUpperCase())}
              />
              <Input
                label="Contact person"
                required
                value={form.contactName}
                onChange={(event) => update('contactName', event.target.value)}
              />
              <Input
                label="Contact phone"
                required
                value={form.contactPhone}
                onChange={(event) => update('contactPhone', event.target.value)}
              />
            </div>
          </Card>

          <Card title="Registered address">
            <div className="grid gap-4 sm:grid-cols-2">
              <Input
                label="Address line 1"
                value={form.addressLine1}
                onChange={(event) => update('addressLine1', event.target.value)}
              />
              <Input
                label="Address line 2"
                value={form.addressLine2}
                onChange={(event) => update('addressLine2', event.target.value)}
              />
              <Input label="City" value={form.city} onChange={(event) => update('city', event.target.value)} />
              <Input label="State" value={form.state} onChange={(event) => update('state', event.target.value)} />
              <Input
                label="Postal code"
                value={form.postalCode}
                onChange={(event) => update('postalCode', event.target.value)}
              />
              <Input
                label="Country"
                value={form.country}
                onChange={(event) => update('country', event.target.value)}
              />
            </div>
          </Card>

          <Card title="Settlement bank account">
            <div className="grid gap-4 sm:grid-cols-2">
              <Input
                label="Account holder"
                value={form.bankAccountName}
                onChange={(event) => update('bankAccountName', event.target.value)}
              />
              <Input
                label="Account number"
                value={form.bankAccountNumber}
                onChange={(event) => update('bankAccountNumber', event.target.value)}
              />
              <Input
                label="IFSC"
                value={form.bankIfsc}
                onChange={(event) => update('bankIfsc', event.target.value.toUpperCase())}
              />
            </div>
          </Card>

          <div className="flex items-center justify-between">
            <p className="text-sm text-slate-500">
              Already registered?{' '}
              <Link className="font-medium text-primary hover:text-primary-dark" to="/login">
                Sign in
              </Link>
            </p>
            <Button type="submit" loading={loading}>
              Create account
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
