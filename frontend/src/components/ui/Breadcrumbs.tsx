import { ChevronRight } from 'lucide-react'
import type { ReactNode } from 'react'

export function Breadcrumbs({ items }: { items: { label: string; element?: ReactNode }[] }) {
  return (
    <nav aria-label="Breadcrumb" className="mb-5 flex items-center gap-1 text-sm text-slate-500">
      {items.map((item, index) => (
        <span className="flex items-center gap-1" key={`${item.label}-${index}`}>
          {index > 0 && <ChevronRight className="h-4 w-4 text-slate-300" />}
          {item.element ?? (
            <span className={index === items.length - 1 ? 'font-medium text-slate-800' : ''}>
              {item.label}
            </span>
          )}
        </span>
      ))}
    </nav>
  )
}
