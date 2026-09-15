import {
  CreditCard,
  FileText,
  LayoutDashboard,
  LogOut,
  Menu,
  ShieldAlert,
  WalletCards,
  X,
} from 'lucide-react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { useState } from 'react'
import { useAuth } from '../../context/AuthContext'

const links = [
  { to: '/dashboard', label: 'Overview', icon: LayoutDashboard },
  { to: '/dashboard/orders', label: 'Orders', icon: FileText },
  { to: '/dashboard/transactions', label: 'Transactions', icon: CreditCard },
  { to: '/dashboard/fraud-alerts', label: 'Fraud alerts', icon: ShieldAlert },
  { to: '/dashboard/refunds', label: 'Refunds', icon: WalletCards },
]

export function DashboardLayout() {
  const { merchant, logout } = useAuth()
  const [open, setOpen] = useState(false)
  return (
    <div className="min-h-screen bg-slate-50">
      <header className="sticky top-0 z-20 border-b border-slate-200 bg-white lg:hidden">
        <div className="flex items-center justify-between px-4 py-3">
          <Link to="/dashboard" className="text-lg font-bold text-primary-dark">
            PayFlux
          </Link>
          <button onClick={() => setOpen(!open)} className="rounded p-2 hover:bg-slate-100">
            {open ? <X /> : <Menu />}
          </button>
        </div>
      </header>
      <div className="mx-auto flex max-w-[1440px]">
        <aside
          className={[
            'fixed inset-y-0 left-0 z-30 w-64 border-r border-slate-200 bg-white p-5',
            'transition-transform lg:sticky lg:top-0 lg:block lg:h-screen lg:translate-x-0',
            open ? 'translate-x-0' : '-translate-x-full',
          ].join(' ')}
        >
          <div className="mb-10 flex items-center justify-between">
            <Link to="/dashboard" className="text-xl font-bold text-primary-dark">
              PayFlux
            </Link>
            <button className="lg:hidden" onClick={() => setOpen(false)}>
              <X />
            </button>
          </div>
          <nav className="space-y-1">
            {links.map(({ to, label, icon: Icon }) => (
              <NavLink
                key={to}
                to={to}
                end={to === '/dashboard'}
                onClick={() => setOpen(false)}
                className={({ isActive }) =>
                  [
                    'flex items-center gap-3 border-l-2 px-3 py-2.5 text-sm',
                    isActive
                      ? 'border-primary bg-primary-light/30 font-medium text-primary-dark'
                      : 'border-transparent text-slate-600 hover:bg-slate-50',
                  ].join(' ')
                }
              >
                <Icon className="h-4 w-4" />
                {label}
              </NavLink>
            ))}
          </nav>
          <div className="absolute bottom-5 left-5 right-5 border-t border-slate-200 pt-4">
            <div className="mb-3 truncate text-sm font-medium text-slate-700">
              {merchant?.businessName}
            </div>
            <button
              onClick={logout}
              className="flex items-center gap-2 text-sm text-slate-500 hover:text-rose-600"
            >
              <LogOut className="h-4 w-4" />
              Sign out
            </button>
          </div>
        </aside>
        {open && (
          <div
            className="fixed inset-0 z-20 bg-slate-900/20 lg:hidden"
            onClick={() => setOpen(false)}
          />
        )}
        <main className="min-w-0 flex-1 p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
