import { formatCell } from '../../utils/format'
import { Panel } from './Panel'
import type { ReactNode } from 'react'

interface TableProps<T extends object> {
  rows?: T[]
  columns?: (keyof T)[]
  action?: (row: T) => ReactNode
}

interface TablePanelProps<T extends object>
  extends TableProps<T> {
  title: string
}

export function TablePanel<T extends object>({
  title,
  rows = [],
  columns = [],
  action
}: TablePanelProps<T>) {
  return (
    <Panel title={title}>
      <Table
        rows={rows}
        columns={columns}
        action={action}
      />
    </Panel>
  )
}

export function Table<T extends object>({
  rows = [],
  columns = [],
  action
}: TableProps<T>) {
  if (!rows.length) {
    return (
      <p className="empty-state">
        Nenhum registro encontrado.
      </p>
    )
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={String(column)}>
                {String(column)}
              </th>
            ))}
            {action && <th>Ações</th>}
          </tr>
        </thead>

        <tbody>
          {rows.map((row, index) => (
            <tr
              key={String(
                (row as { id?: unknown; orderId?: unknown }).id ??
                (row as { orderId?: unknown }).orderId ??
                index
              )}
            >
              {columns.map((column) => (
                <td key={String(column)}>
                  {formatCell(
                    row[column as keyof T]
                  )}
                </td>
              ))}
              {action && <td>{action(row)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}