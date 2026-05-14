import type { ReactNode } from 'react'

interface ListProps<T extends { id?: number | string }> {
  items?: T[]
  render: (item: T) => ReactNode
  action?: (item: T) => ReactNode
}

export function List<T extends { id?: number | string }>({
  items = [],
  render,
  action
}: ListProps<T>) {
  if (!items.length) {
    return (
      <p className="empty-state">
        Nenhum item cadastrado.
      </p>
    )
  }

  return (
    <div className="simple-list">
      {items.map((item, index) => (
        <div key={item.id ?? index}>
          <span>{render(item)}</span>
          {action?.(item)}
        </div>
      ))}
    </div>
  )
}