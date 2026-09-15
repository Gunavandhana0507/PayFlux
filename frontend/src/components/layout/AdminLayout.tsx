import { Link, NavLink, Outlet } from 'react-router-dom'
import { ShieldCheck } from 'lucide-react'

export function AdminLayout() {
  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-5 py-4">
          <Link to="/admin" className="flex items-center gap-2 text-xl font-bold text-primary-dark">
            <ShieldCheck className="h-5 w-5" />
            PayFlux Admin
          </Link>
          <nav className="flex gap-4 text-sm text-slate-600">
            {[
              ['/admin', 'Overview'],
              ['/admin/merchants', 'Merchants'],
              ['/admin/transactions', 'Transactions'],
              ['/admin/fraud-alerts', 'Fraud alerts'],
            ].map(([to, label]) => (
              <NavLink
                key={to}
                to={to}
                end={to === '/admin'}
                className={({ isActive }) =>
                  isActive ? 'font-medium text-primary-dark' : 'hover:text-primary'
                }
              >
                {label}
              </NavLink>
            ))}
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-6xl p-5 sm:p-8">
        <Outlet />
      </main>
    </div>
  )
}
