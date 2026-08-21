import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiErrorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { Logo } from '../components/AppLayout'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { ErrorState } from '../components/ui/Feedback'
import { Input } from '../components/ui/Input'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      const profile = await login(email, password)
      navigate(profile.role === 'ADMIN' ? '/admin' : '/dashboard', { replace: true })
    } catch (err) {
      setError(apiErrorMessage(err, 'Invalid email or password'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-full items-center justify-center bg-slate-50 px-4 py-12">
      <div className="w-full max-w-md">
        <div className="mb-6 flex justify-center">
          <Logo subtitle="Merchant dashboard" />
        </div>
        <Card title="Sign in" description="Use your PayFlux merchant credentials.">
          <form className="space-y-4" onSubmit={onSubmit}>
            {error && <ErrorState message={error} />}
            <Input
              label="Email"
              type="email"
              required
              value={email}
              autoComplete="email"
              onChange={(event) => setEmail(event.target.value)}
            />
            <Input
              label="Password"
              type="password"
              required
              value={password}
              autoComplete="current-password"
              onChange={(event) => setPassword(event.target.value)}
            />
            <Button type="submit" className="w-full" loading={loading}>
              Sign in
            </Button>
          </form>
          <p className="mt-4 text-center text-sm text-slate-500">
            New to PayFlux?{' '}
            <Link className="font-medium text-primary hover:text-primary-dark" to="/signup">
              Create a merchant account
            </Link>
          </p>
        </Card>
      </div>
    </div>
  )
}
