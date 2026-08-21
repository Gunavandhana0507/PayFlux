import clsx from 'clsx'
import type { ReactNode } from 'react'

export function LoadingSpinner({ className, label }: { className?: string; label?: string }) {
  return (
    <div className={clsx('flex items-center justify-center gap-3 py-10 text-slate-500', className)}>
      <span className="h-5 w-5 animate-spin rounded-full border-2 border-primary border-t-transparent" />
      {label && <span className="text-sm">{label}</span>}
    </div>
  )
}

export function EmptyState({
  title,
  description,
  action,
}: {
  title: string
  description?: string
  action?: ReactNode
}) {
  return (
    <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-slate-200 bg-slate-50/60 px-6 py-12 text-center">
      <p className="text-sm font-semibold text-slate-700">{title}</p>
      {description && <p className="mt-1 max-w-md text-sm text-slate-500">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}

export function ErrorState({ message, action }: { message: string; action?: ReactNode }) {
  return (
    <div className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
      <p>{message}</p>
      {action && <div className="mt-3">{action}</div>}
    </div>
  )
}
