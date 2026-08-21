import clsx from 'clsx'
import type { ReactNode } from 'react'

interface CardProps {
  title?: ReactNode
  description?: ReactNode
  actions?: ReactNode
  className?: string
  bodyClassName?: string
  children?: ReactNode
}

export function Card({ title, description, actions, className, bodyClassName, children }: CardProps) {
  return (
    <section className={clsx('rounded-lg border border-slate-200 bg-white shadow-card', className)}>
      {(title || actions) && (
        <header className="flex items-start justify-between gap-4 border-b border-slate-100 px-5 py-4">
          <div>
            {title && <h2 className="text-base font-semibold text-slate-800">{title}</h2>}
            {description && <p className="mt-0.5 text-sm text-slate-500">{description}</p>}
          </div>
          {actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
        </header>
      )}
      <div className={clsx('px-5 py-4', bodyClassName)}>{children}</div>
    </section>
  )
}

export function StatCard({
  label,
  value,
  sub,
}: {
  label: string
  value: ReactNode
  sub?: ReactNode
}) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white px-5 py-4 shadow-card">
      <p className="text-sm text-slate-500">{label}</p>
      <p className="mt-1 text-2xl font-semibold text-slate-800">{value}</p>
      {sub && <p className="mt-1 text-xs text-slate-500">{sub}</p>}
    </div>
  )
}
