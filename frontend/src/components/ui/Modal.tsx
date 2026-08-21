import type { ReactNode } from 'react'

interface ModalProps {
  open: boolean
  title: string
  description?: string
  onClose: () => void
  footer?: ReactNode
  children: ReactNode
}

export function Modal({ open, title, description, onClose, footer, children }: ModalProps) {
  if (!open) return null
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-slate-900/40"
        role="presentation"
        onClick={onClose}
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className="relative w-full max-w-lg rounded-lg bg-white shadow-card"
      >
        <header className="border-b border-slate-100 px-5 py-4">
          <h2 className="text-base font-semibold text-slate-800">{title}</h2>
          {description && <p className="mt-0.5 text-sm text-slate-500">{description}</p>}
        </header>
        <div className="px-5 py-4">{children}</div>
        {footer && (
          <footer className="flex justify-end gap-2 border-t border-slate-100 px-5 py-4">{footer}</footer>
        )}
      </div>
    </div>
  )
}
