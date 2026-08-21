import clsx from 'clsx'
import type { InputHTMLAttributes, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'

const fieldClasses =
  'w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-800 shadow-sm ' +
  'placeholder:text-slate-400 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary-light ' +
  'disabled:bg-slate-50 disabled:text-slate-400'

interface FieldProps {
  label?: string
  hint?: string
  error?: string
}

function FieldShell({
  label,
  hint,
  error,
  children,
}: FieldProps & { children: React.ReactNode }) {
  return (
    <label className="block">
      {label && <span className="mb-1 block text-sm font-medium text-slate-700">{label}</span>}
      {children}
      {error ? (
        <span className="mt-1 block text-xs text-rose-600">{error}</span>
      ) : (
        hint && <span className="mt-1 block text-xs text-slate-500">{hint}</span>
      )}
    </label>
  )
}

export function Input({
  label,
  hint,
  error,
  className,
  ...rest
}: FieldProps & InputHTMLAttributes<HTMLInputElement>) {
  return (
    <FieldShell label={label} hint={hint} error={error}>
      <input {...rest} className={clsx(fieldClasses, error && 'border-rose-300', className)} />
    </FieldShell>
  )
}

export function Select({
  label,
  hint,
  error,
  className,
  children,
  ...rest
}: FieldProps & SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <FieldShell label={label} hint={hint} error={error}>
      <select {...rest} className={clsx(fieldClasses, className)}>
        {children}
      </select>
    </FieldShell>
  )
}

export function Textarea({
  label,
  hint,
  error,
  className,
  ...rest
}: FieldProps & TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <FieldShell label={label} hint={hint} error={error}>
      <textarea {...rest} className={clsx(fieldClasses, className)} />
    </FieldShell>
  )
}
