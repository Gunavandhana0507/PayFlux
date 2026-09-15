import type { InputHTMLAttributes, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'

interface FieldProps {
  label: string
  error?: string
  hint?: string
  required?: boolean
}

export function Input({ label, error, hint, required, className = '', ...props }: FieldProps & InputHTMLAttributes<HTMLInputElement>) {
  return (
    <label className="block space-y-1.5">
      <span className="text-sm font-medium text-slate-700">{label}{required && <span className="text-rose-600"> *</span>}</span>
      <input {...props} className={`w-full rounded-md border px-3 py-2 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 ${error ? 'border-rose-400' : 'border-slate-300'} ${className}`} />
      {hint && !error && <span className="block text-xs text-slate-500">{hint}</span>}
      {error && <span className="block text-xs text-rose-600">{error}</span>}
    </label>
  )
}

export function Select({ label, error, hint, required, children, className = '', ...props }: FieldProps & SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <label className="block space-y-1.5">
      <span className="text-sm font-medium text-slate-700">{label}{required && <span className="text-rose-600"> *</span>}</span>
      <select {...props} className={`w-full rounded-md border bg-white px-3 py-2 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 ${error ? 'border-rose-400' : 'border-slate-300'} ${className}`}>{children}</select>
      {hint && !error && <span className="block text-xs text-slate-500">{hint}</span>}
      {error && <span className="block text-xs text-rose-600">{error}</span>}
    </label>
  )
}

export function Textarea({ label, error, hint, required, className = '', ...props }: FieldProps & TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <label className="block space-y-1.5">
      <span className="text-sm font-medium text-slate-700">{label}{required && <span className="text-rose-600"> *</span>}</span>
      <textarea {...props} className={`w-full rounded-md border px-3 py-2 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 ${error ? 'border-rose-400' : 'border-slate-300'} ${className}`} />
      {hint && !error && <span className="block text-xs text-slate-500">{hint}</span>}
      {error && <span className="block text-xs text-rose-600">{error}</span>}
    </label>
  )
}
