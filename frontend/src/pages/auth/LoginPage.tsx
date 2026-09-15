import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { Button } from '../../components/ui/Button'
import { Input } from '../../components/ui/Input'
import { getFriendlyError } from '../../lib/errors'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../components/ui/Toast'

export function LoginPage() {
  const { login } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [form, setForm] = useState({ email: '', password: '' })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const submit = async (event: React.FormEvent) => {
    event.preventDefault()
    const next: Record<string, string> = {}
    if (!form.email.includes('@')) next.email = 'Enter a valid email address'
    if (!form.password) next.password = 'Enter your password'
    setErrors(next)
    if (Object.keys(next).length) return
    setLoading(true)
    try {
      await login(form)
      toast.show('Welcome to PayFlux', 'success')
      navigate(params.get('from') || '/dashboard', { replace: true })
    } catch (error) {
      const friendly = getFriendlyError(error)
      setMessage(friendly.message)
      setErrors(friendly.fieldErrors)
    } finally { setLoading(false) }
  }
  return <div className="mx-auto flex min-h-[calc(100vh-73px)] max-w-md items-center px-5 py-10"><div className="card w-full"><h1 className="text-2xl font-semibold text-slate-900">Welcome back</h1><p className="mt-1 text-sm text-slate-500">Sign in to manage your payments.</p>{message && <div className="mt-5 rounded-md bg-rose-50 p-3 text-sm text-rose-700">{message}</div>}<form onSubmit={submit} className="mt-6 space-y-4"><Input label="Email" type="email" value={form.email} error={errors.email} required onChange={(event) => setForm({ ...form, email: event.target.value })} /><Input label="Password" type="password" value={form.password} error={errors.password} required onChange={(event) => setForm({ ...form, password: event.target.value })} /><Button type="submit" loading={loading} className="w-full">Sign in</Button></form><p className="mt-6 text-center text-sm text-slate-500">New to PayFlux? <Link to="/register" className="font-medium text-primary-dark hover:underline">Create an account</Link></p></div></div>
}
