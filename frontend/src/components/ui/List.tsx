/* eslint-disable @typescript-eslint/ban-ts-comment */
// @ts-nocheck
export function List({ items = [], render, action }) {
  if (!items.length) return <p className="empty-state">Nenhum item cadastrado.</p>
  return (
    <div className="simple-list">
      {items.map((item) => (
        <div key={item.id}>
          <span>{render(item)}</span>
          {action?.(item)}
        </div>
      ))}
    </div>
  )
}
