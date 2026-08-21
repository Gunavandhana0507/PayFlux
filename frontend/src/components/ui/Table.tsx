import clsx from 'clsx'
import type { ReactNode } from 'react'

export interface Column<T> {
  key: string
  header: ReactNode
  render: (row: T) => ReactNode
  className?: string
}

interface TableProps<T> {
  columns: Column<T>[]
  rows: T[]
  rowKey: (row: T) => string
  onRowClick?: (row: T) => void
  empty?: ReactNode
}

export function Table<T>({ columns, rows, rowKey, onRowClick, empty }: TableProps<T>) {
  if (rows.length === 0 && empty) {
    return <>{empty}</>
  }
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-slate-200 text-sm">
        <thead>
          <tr className="text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
            {columns.map((column) => (
              <th key={column.key} scope="col" className={clsx('px-4 py-3', column.className)}>
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {rows.map((row) => (
            <tr
              key={rowKey(row)}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              className={clsx('text-slate-700', onRowClick && 'cursor-pointer hover:bg-slate-50')}
            >
              {columns.map((column) => (
                <td key={column.key} className={clsx('px-4 py-3 align-middle', column.className)}>
                  {column.render(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
