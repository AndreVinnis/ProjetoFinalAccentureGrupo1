/* eslint-disable @typescript-eslint/ban-ts-comment */
// @ts-nocheck
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Navigate, NavLink, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import './App.css'
import { LoadingScreen } from './components/layout/LoadingScreen'
import { RoleNavigation } from './components/layout/RoleNavigation'
import { AuthPanel } from './features/auth/AuthPanel'
import { AdminBank } from './features/banking/AdminBank'
import { CustomerBank } from './features/banking/CustomerBank'
import { Cart } from './features/ecommerce/pages/Cart'
import { Orders } from './features/ecommerce/pages/Orders'
import { Profile } from './features/ecommerce/pages/Profile'
import { Storefront } from './features/ecommerce/pages/Storefront'
import { createApi } from './services/api'
import { defaultPathForRoles, isAdmin, titleForPath } from './utils/auth'
import { AdminEcommerce } from './features/ecommerce/pages/admin/AdminEcommerce'

function TopbarIcon({ name }) {
  const paths = {
    store: (
      <>
        <path d="M4 10h16" />
        <path d="M5 10l1.2-5h11.6L19 10" />
        <path d="M6 10v9h12v-9" />
        <path d="M9 19v-5h6v5" />
      </>
    ),
    cart: (
      <>
        <path d="M5 5h2l1.4 8.2a2 2 0 0 0 2 1.8h6.9a2 2 0 0 0 1.9-1.4L21 8H8" />
        <path d="M10 19h.01" />
        <path d="M18 19h.01" />
      </>
    ),
    user: (
      <>
        <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" />
        <path d="M4 20a8 8 0 0 1 16 0" />
      </>
    ),
    orders: (
      <>
        <path d="M6 3h12v18l-2-1.2-2 1.2-2-1.2-2 1.2-2-1.2L6 21V3Z" />
        <path d="M9 8h6" />
        <path d="M9 12h6" />
      </>
    ),
  }

  return (
    <svg className="topbar-action-icon" viewBox="0 0 24 24" aria-hidden="true">
      {paths[name]}
    </svg>
  )
}

function App() {
  const [session, setSession] = useState(() => {
    const saved = localStorage.getItem('acc_session')
    return saved ? JSON.parse(saved) : null
  })
  const [toastMessage, setToast] = useState('')
  const [bootLoading, setBootLoading] = useState(true)
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)

  const clearSession = useCallback(() => {
    localStorage.removeItem('acc_session')
    setSession(null)
  }, [])

  const api = useMemo(() => createApi(session?.token, setToast, clearSession), [session?.token, clearSession])
  const roles = useMemo(() => session?.roles || [], [session?.roles])
  const admin = isAdmin(roles)
  const customer = roles.includes('CUSTOMER')

  useEffect(() => {
    if (!session) return
    localStorage.setItem('acc_session', JSON.stringify(session))
  }, [session])

  useEffect(() => {
    const timer = setTimeout(() => setBootLoading(false), 1500)
    return () => clearTimeout(timer)
  }, [])

  useEffect(() => {
    if (!toastMessage) return
    const t = setTimeout(() => setToast(''), 4200)
    return () => clearTimeout(t)
  }, [toastMessage])

  function handleLogout() {
    clearSession()
  }

  function handleAuthMouseMove(event) {
    if (session) return
    const rect = event.currentTarget.getBoundingClientRect()
    const x = ((event.clientX - rect.left) / rect.width) * 100
    const y = ((event.clientY - rect.top) / rect.height) * 100
    event.currentTarget.style.setProperty('--mouse-x', `${x}%`)
    event.currentTarget.style.setProperty('--mouse-y', `${y}%`)
  }

  function RequireAuth({ children }) {
    const location = useLocation()
    if (!session) return <Navigate to="/login" replace state={{ from: location }} />
    return children
  }

  function RequireAdmin({ children }) {
    if (!admin) return <Navigate to="/banco" replace />
    return children
  }

  function RequireCustomer({ children }) {
    if (!customer) return <Navigate to={admin ? '/admin' : '/login'} replace />
    return children
  }

  function AppShell() {
    const location = useLocation()
    return (
      <main className={`app-shell ${sidebarCollapsed ? 'sidebar-collapsed' : ''}`}>
        <aside className="sidebar">
          <button
            className="sidebar-toggle"
            onClick={() => setSidebarCollapsed((current) => !current)}
            aria-label="Alternar menu lateral"
          >
            <span />
            <span />
            <span />
          </button>
          <div className="brand-block">
            <div className="brand-mark">ACC</div>
            <div className="sidebar-copy">
              <strong>ACC Bank</strong>
              <span>Banco + Ecommerce</span>
            </div>
          </div>
          <div className="session-card">
            <span>Logado como</span>
            <strong>{session.name}</strong>
            <small>{admin ? 'Administrador' : 'Cliente'} - {roles.join(' + ')}</small>
          </div>
          <RoleNavigation roles={roles} />
          <button className="ghost-button logout-button" onClick={handleLogout}>Sair</button>
        </aside>

        <section className="workspace">
          <header className="topbar">
            <div>
              <p className="eyebrow">Plataforma integrada</p>
              <h1>{titleForPath(location.pathname)}</h1>
            </div>
            {customer && ['/loja', '/carrinho', '/perfil', '/pedidos'].some((p) => location.pathname.startsWith(p)) ? (
              <div className="topbar-actions">
                <NavLink to="/loja" className={({ isActive }) => (isActive ? 'active' : '')}>
                  <TopbarIcon name="store" />
                  <span>Vitrine</span>
                </NavLink>
                <NavLink to="/carrinho" className={({ isActive }) => (isActive ? 'active' : '')}>
                  <TopbarIcon name="cart" />
                  <span>Sacola</span>
                </NavLink>
                <NavLink to="/pedidos" className={({ isActive }) => (isActive ? 'active' : '')}>
                  <TopbarIcon name="orders" />
                  <span>Pedidos</span>
                </NavLink>
                <NavLink to="/perfil" className={({ isActive }) => (isActive ? 'active' : '')}>
                  <TopbarIcon name="user" />
                  <span>Minha conta</span>
                </NavLink>
              </div>
            ) : null}
          </header>

          {toastMessage ? <div className="toast-banner" role="status">{toastMessage}</div> : null}

          <Outlet />
        </section>
      </main>
    )
  }

  function AuthLayout() {
    return (
      <main className="app-shell auth-only">
        <aside className="sidebar">
          <div className="brand-block">
            <div className="brand-mark">ACC</div>
            <div className="sidebar-copy">
              <strong>ACC Bank</strong>
              <span>Banco + Ecommerce</span>
            </div>
          </div>
        </aside>
        <section className="workspace auth-workspace" onMouseMove={handleAuthMouseMove}>
          <header className="topbar">
            <div>
              <p className="eyebrow">Plataforma integrada</p>
              <h1>Acesso</h1>
            </div>
          </header>

          {toastMessage ? <div className="toast-banner" role="status">{toastMessage}</div> : null}

          <Outlet />
        </section>
      </main>
 )
  }

  return (
    <>
      {bootLoading && (
        <LoadingScreen
          title="Preparando sua experiencia ACC"
          detail="Conectando banco e ecommerce"
        />
      )}

      <Routes>
        {/* Rota publica de autenticacao */}
        <Route element={<AuthLayout />}>
          <Route
            path="/login"
            element={
              session
                ? <Navigate to={defaultPathForRoles(roles)} replace />
                : <AuthPanel api={api} setSession={setSession} setToast={setToast} />
            }
          />
        </Route>

        {/* Rotas protegidas com layout completo */}
        <Route
          element={
            <RequireAuth>
              <AppShell />
            </RequireAuth>
          }
        >
          <Route
            path="/banco"
            element={
              <RequireCustomer>
                <CustomerBank api={api} />
              </RequireCustomer>
            }
          />
          <Route path="/loja" element={<Storefront api={api} />} />
          <Route path="/carrinho" element={<Cart api={api} />} />
          <Route path="/perfil" element={<Profile api={api} />} />
          <Route path="/pedidos" element={<Orders api={api} />} />

          {/* Admin */}
          <Route
            path="/admin"
            element={
              <RequireAdmin>
                <div className="dashboard-grid">
                  <p>Painel admin — selecione uma area no menu lateral.</p>
                </div>
              </RequireAdmin>
            }
          />
          <Route
            path="/admin/banco"
            element={
              <RequireAdmin>
                <AdminBank api={api} />
              </RequireAdmin>
            }
          />
          <Route
            path="/admin/ecommerce"
            element={
              <RequireAdmin>
                <div className="dashboard-grid">
                  <AdminEcommerce api={api} />
              </div>
              </RequireAdmin>
            }
          />
        </Route>

        {/* Landing e fallback */}
        <Route
          path="/"
          element={
            session
              ? <Navigate to={defaultPathForRoles(roles)} replace />
              : <Navigate to="/login" replace />
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  )
}

export default App
