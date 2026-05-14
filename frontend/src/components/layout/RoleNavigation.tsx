import { isAdmin } from '../../utils/auth'

interface RoleNavigationProps {
  activeView: string;
  roles: string[];
  setActiveView: (view: string) => void;
}

type NavItem = {
  id: string;
  label: string;
};

export function RoleNavigation({ activeView, roles, setActiveView }: RoleNavigationProps) {
  const admin = isAdmin(roles);

  const items: NavItem[] = admin
    ? ([
        { id: 'adminHome', label: 'Painel admin' },
        roles.includes('ECOMMERCE_ADMIN') ? { id: 'adminEcommerce', label: 'Gestao ecommerce' } : null,
        roles.includes('BANKING_ADMIN') ? { id: 'adminBank', label: 'Gestao banco' } : null,
      ].filter(Boolean) as NavItem[])
    : [
        { id: 'storefront', label: 'Loja' },
        { id: 'customerBank', label: 'Banco' },
      ];

  return (
    <nav className="nav-list">
      {items.map((item) => (
        <button 
          key={item.id} 
          className={activeView === item.id ? 'active' : ''} 
          onClick={() => setActiveView(item.id)}
        >
          {item.label}
        </button>
      ))}
    </nav>
  )
}