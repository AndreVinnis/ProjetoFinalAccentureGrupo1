/* eslint-disable @typescript-eslint/ban-ts-comment */
// @ts-nocheck
import { isAdmin } from '../../utils/auth'

export function RoleNavigation({ activeView, roles, setActiveView }) {
  const admin = isAdmin(roles)
  const items = admin
    ? [
        { id: 'adminHome', label: 'Painel admin' },
        roles.includes('ECOMMERCE_ADMIN') && { id: 'adminEcommerce', label: 'Gestao ecommerce' },
        roles.includes('BANKING_ADMIN') && { id: 'adminBank', label: 'Gestao banco' },
      ].filter(Boolean)
    : [
        { id: 'customerHome', label: 'Feed' },
        { id: 'customerEcommerce', label: 'Loja' },
        { id: 'customerBank', label: 'Banco' },
      ]

  return (
    <nav className="nav-list">
      {items.map((item) => (
        <button key={item.id} className={activeView === item.id ? 'active' : ''} onClick={() => setActiveView(item.id)}>
          {item.label}
        </button>
      ))}
    </nav>
  )
}
