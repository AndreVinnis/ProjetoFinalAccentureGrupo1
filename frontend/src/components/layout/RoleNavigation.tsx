import { NavLink } from 'react-router-dom'
import { isAdmin } from '../../utils/auth'

interface RoleNavigationProps {
  roles: string[];
}

type NavItem = {
  path: string;
  label: string;
};

export function RoleNavigation({ roles }: RoleNavigationProps) {
  const admin = isAdmin(roles);

  const items: NavItem[] = admin
    ? ([
        { path: '/admin', label: 'Painel admin' },
        roles.includes('ECOMMERCE_ADMIN') ? { path: '/admin/ecommerce', label: 'Gestao ecommerce' } : null,
        roles.includes('BANKING_ADMIN') ? { path: '/admin/banco', label: 'Gestao banco' } : null,
      ].filter(Boolean) as NavItem[])
    : [
        { path: '/banco', label: 'Banco' },
        { path: '/loja', label: 'Loja' },
      ];

  return (
    <nav className="nav-list">
      {items.map((item) => (
        <NavLink
          key={item.path}
          to={item.path}
          end={item.path === '/admin'}
          className={({ isActive }) => (isActive ? 'active' : '')}
        >
          {item.label}
        </NavLink>
      ))}
    </nav>
  )
}
