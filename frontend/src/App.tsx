/* eslint-disable @typescript-eslint/ban-ts-comment */
// @ts-nocheck
import { useEffect, useMemo, useState } from 'react'
import './App.css'
import { LoadingScreen } from './components/layout/LoadingScreen'
import { RoleNavigation } from './components/layout/RoleNavigation'
import { AuthPanel } from './features/auth/AuthPanel'
import { AdminBank } from './features/banking/AdminBank'
import { CustomerBank } from './features/banking/CustomerBank'
import { Cart } from './features/ecommerce/pages/Cart'
import { Storefront } from './features/ecommerce/pages/Storefront'
import { Profile } from './features/ecommerce/pages/Profile'
import { AdminHome, CustomerHome } from './features/home/HomeViews'
import { createApi } from './services/api'
import { defaultViewForRoles, isAdmin, viewTitle } from './utils/auth'

function App() {
  const [session, setSession] = useState(() => {
    const saved = localStorage.getItem('acc_session')
    return saved ? JSON.parse(saved) : null
  })
  const [mode, setMode] = useState('login')
  const [activeView, setActiveView] = useState('customerHome')
  const [toastMessage, setToast] = useState('')
  const [bootLoading, setBootLoading] = useState(true)
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)

  const api = useMemo(() => createApi(session?.token, setToast), [session?.token])
  const roles = useMemo(() => session?.roles || [], [session?.roles])
  const admin = isAdmin(roles)
  const customer = roles.includes('CUSTOMER')

  function handleAuthMouseMove(event) {
    if (session) return
    const rect = event.currentTarget.getBoundingClientRect()
    const x = ((event.clientX - rect.left) / rect.width) * 100
    const y = ((event.clientY - rect.top) / rect.height) * 100
    event.currentTarget.style.setProperty('--mouse-x', `${x}%`)
    event.currentTarget.style.setProperty('--mouse-y', `${y}%`)
  }

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

  function applySession(nextSession) {
    setSession(nextSession)
    setActiveView(defaultViewForRoles(nextSession.roles || []))
  }

  function handleLogout() {
    localStorage.removeItem('acc_session')
    setSession(null)
    setMode('login')
  }

  return (
    <main className={`app-shell ${sidebarCollapsed ? 'sidebar-collapsed' : ''}`}>
      {bootLoading && <LoadingScreen title="Preparando sua experiencia ACC" detail="Conectando banco e ecommerce" />}
      <aside className="sidebar">
        {session && (
          <button className="sidebar-toggle" onClick={() => setSidebarCollapsed((current) => !current)} aria-label="Alternar menu lateral">
            <span />
            <span />
            <span />
          </button>
        )}
        <div className="brand-block">
          <div className="brand-mark">ACC</div>
          <div className="sidebar-copy">
            <strong>ACC Bank</strong>
            <span>Banco + Ecommerce</span>
          </div>
        </div>
        {session ? (
          <>
            <div className="session-card">
              <span>Logado como</span>
              <strong>{session.name}</strong>
              <small>{admin ? 'Administrador' : 'Cliente'} - {roles.join(' + ')}</small>
            </div>
            <RoleNavigation activeView={activeView} roles={roles} setActiveView={setActiveView} />
            <button className="ghost-button logout-button" onClick={handleLogout}>Sair</button>
          </>
        ) : null}
      </aside>

      <section className={`workspace ${!session ? 'auth-workspace' : ''}`} onMouseMove={handleAuthMouseMove}>
        <header className="topbar">
          <div>
            <p className="eyebrow">Plataforma integrada</p>
            <h1>{viewTitle(activeView)}</h1>
          </div>
          {session && customer ? (
            <div className="topbar-actions">
              <button className={activeView === 'storefront' ? 'active' : ''} onClick={() => setActiveView('storefront')}>Vitrine</button>
              <button className={activeView === 'cart' ? 'active' : ''} onClick={() => setActiveView('cart')}>Carrinho</button>
              <button className={activeView === 'profile' ? 'active' : ''} onClick={() => setActiveView('profile')}>Minhas informações</button>
            </div>
          ) : null}
        </header>

        {toastMessage ? <div className="toast-banner" role="status">{toastMessage}</div> : null}

        {!session ? (
          <AuthPanel api={api} mode={mode} setMode={setMode} setSession={applySession} setToast={setToast} />
        ) : (
          <>
            {activeView === 'customerHome' && <CustomerHome api={api} setActiveView={setActiveView} setToast={setToast} />}
            {activeView === 'adminHome' && <AdminHome roles={roles} setActiveView={setActiveView} />}
            {activeView === 'customerBank' && <CustomerBank api={api} />}
            {activeView === 'storefront' && <Storefront api={api} />}
            {activeView === 'cart' && <Cart api={api} />}
            {activeView === 'profile' && <Profile api={api} />}
            {activeView === 'adminEcommerce' && <AdminEcommerce api={api} />}
            {activeView === 'adminBank' && <AdminBank api={api} />}
          </>
        )}
      </section>
    </main>
  )
}

export default App