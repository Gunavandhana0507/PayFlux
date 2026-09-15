import type { ReactNode } from 'react'

export interface Column<T> {
  key: string
  header: string
  render: (row: T) => ReactNode
  className?: string
}

export function Table<T>({
  columns,
  rows,
  onRowClick,
}: {
  columns: Column<T>[]
  rows: T[]
  onRowClick?: (row: T) => void
}) {
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full text-left text-sm">
        <thead className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500">
          <tr>
            {columns.map((column) => (
              <th
                key={column.key}
                className={`whitespace-nowrap px-3 py-3 font-medium ${column.className ?? ''}`}
              >
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {rows.map((row, index) => (
            <tr
              key={index}
              onClick={() => onRowClick?.(row)}
              className={onRowClick ? 'cursor-pointer hover:bg-slate-50' : ''}
            >
              {columns.map((column) => (
                <td
                  key={column.key}
                  className={`whitespace-nowrap px-3 py-3 ${column.className ?? ''}`}
                >
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
