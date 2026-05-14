/* eslint-disable @typescript-eslint/ban-ts-comment */
// @ts-nocheck
import { formatCell } from '../../utils/format'
import { Panel } from './Panel'

export function TablePanel({ title, rows = [], columns = [], action }) {
  return (
    <Panel title={title}>
      <Table rows={rows} columns={columns} action={action} />
    </Panel>
  )
}

export function Table({ rows = [], columns = [], action }) {
  if (!rows?.length) return <p className="empty-state">Nenhum registro encontrado.</p>
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((column) => <th key={column}>{column}</th>)}
            {action && <th>Acoes</th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={row.id || row.orderId || index}>
              {columns.map((column) => <td key={column}>{formatCell(row[column])}</td>)}
              {action && <td>{action(row)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
