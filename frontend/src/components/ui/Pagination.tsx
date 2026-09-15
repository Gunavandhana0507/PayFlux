import { Button } from './Button'

export function Pagination({ page, size, total, onChange }: { page: number; size: number; total: number; onChange: (page: number) => void }) {
  const pages = Math.max(1, Math.ceil(total / size))
  if (pages <= 1) return null
  return <div className="mt-5 flex items-center justify-between text-sm text-slate-500"><span>Page {page + 1} of {pages}</span><div className="flex gap-2"><Button size="sm" variant="secondary" disabled={page === 0} onClick={() => onChange(page - 1)}>Previous</Button><Button size="sm" variant="secondary" disabled={page >= pages - 1} onClick={() => onChange(page + 1)}>Next</Button></div></div>
}
