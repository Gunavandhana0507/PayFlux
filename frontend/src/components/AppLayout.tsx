import clsx from 'clsx'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Button } from './ui/Button'

export interface NavItem {
  to: string
  label: string
}

export function Logo({ subtitle }: { subtitle?: string }) {
  return (
    <div className="flex items-center gap-2">
      <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-sm font-bold text-white">
        PF
      </span>
      <span>
        <span className="block text-sm font-semibold text-slate-800">PayFlux</span>
        {subtitle && <span className="block text-xs text-slate-500">{subtitle}</span>}
      </span>
    </div>
  )
}

export function AppLayout({ items, subtitle }: { items: NavItem[]; subtitle: string }) {
  const { profile, logout } = useAuth()
  const navigate = useNavigate()

  return (
    <div className="flex min-h-full bg-slate-50">
      <aside className="hidden w-60 shrink-0 border-r border-slate-200 bg-white px-4 py-6 lg:block">
        <Logo subtitle={subtitle} />
        <nav className="mt-8 flex flex-col gap-1">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to.split('/').length <= 2}
              className={({ isActive }) =>
                clsx(
                  'rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-primary-light/40 text-primary-dark'
                    : 'text-slate-600 hover:bg-slate-100 hover:text-slate-800',
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between gap-4 border-b border-slate-200 bg-white px-6 py-4">
          <div className="lg:hidden">
            <Logo subtitle={subtitle} />
          </div>
          <nav className="flex gap-2 overflow-x-auto lg:hidden">
            {items.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to.split('/').length <= 2}
                className={({ isActive }) =>
                  clsx(
                    'whitespace-nowrap rounded-lg px-2.5 py-1.5 text-xs font-medium',
                    isActive ? 'bg-primary-light/40 text-primary-dark' : 'text-slate-600',
                  )
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
          <div className="ml-auto flex items-center gap-3">
            <div className="hidden text-right sm:block">
              <p className="text-sm font-medium text-slate-800">{profile?.businessName}</p>
              <p className="text-xs text-slate-500">{profile?.email}</p>
            </div>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => {
                logout()
                navigate('/login')
              }}
            >
              Sign out
            </Button>
          </div>
        </header>

        <main className="flex-1 px-6 py-8">
          <div className="mx-auto max-w-6xl">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
