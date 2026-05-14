/* eslint-disable @typescript-eslint/ban-ts-comment */
// @ts-nocheck
/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useMemo, useState } from 'react'
import './App.css'
import { placeholderImageForCategory } from './categoryPlaceholder'

const API_BASE = import.meta.env.VITE_API_URL || '/api'

const initialRegister = {
  name: '',
  email: '',
  password: '',
  cpf: '',
  birthDate: '',
  phone: '',
  zipCode: '',
  state: '',
  city: '',
  neighborhood: '',
  street: '',
  number: '',
  complement: '',
}

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
        </header>

        {toastMessage ? (
          <div className="toast-banner" role="status">
            {toastMessage}
          </div>
        ) : null}

        {!session ? (
          <AuthPanel api={api} mode={mode} setMode={setMode} setSession={applySession} setToast={setToast} />
        ) : (
          <>
            {activeView === 'customerHome' && <CustomerHome api={api} setActiveView={setActiveView} setToast={setToast} />}
            {activeView === 'adminHome' && <AdminHome roles={roles} setActiveView={setActiveView} />}
            {activeView === 'customerBank' && <CustomerBank api={api} />}
            {activeView === 'customerEcommerce' && <CustomerEcommerce api={api} />}
            {activeView === 'adminEcommerce' && <AdminEcommerce api={api} />}
            {activeView === 'adminBank' && <AdminBank api={api} />}
          </>
        )}
      </section>
    </main>
  )
}

function createApi(token, setToast) {
  async function request(path, options = {}) {
    const headers = { ...(options.headers || {}) }
    if (options.body !== undefined) headers['Content-Type'] = 'application/json'
    if (token) headers.Authorization = `Bearer ${token}`

    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    })

    const text = await response.text()
    const payload = text ? safeJson(text) : null
    if (!response.ok) {
      const message = payload?.message || payload?.error || text || `Erro ${response.status}`
      setToast(message)
      const error = new Error(message)
      error.status = response.status
      error.payload = payload
      throw error
    }
    return payload ?? text
  }

  return {
    get: (path) => request(path),
    post: (path, body) => request(path, { method: 'POST', body }),
    put: (path, body) => request(path, { method: 'PUT', body }),
    patch: (path, body) => request(path, { method: 'PATCH', body }),
    delete: (path) => request(path, { method: 'DELETE' }),
  }
}

function safeJson(text) {
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

function RoleNavigation({ activeView, roles, setActiveView }) {
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

function LoadingScreen({ title, detail, compact = false }) {
  return (
    <div className={`loading-screen ${compact ? 'compact' : ''}`}>
      <div className="loading-card">
        <div className="loading-mark">
          <span />
          <span />
          <span />
        </div>
        <strong>{title}</strong>
        <small>{detail}</small>
      </div>
    </div>
  )
}

function AuthPanel({ api, mode, setMode, setSession, setToast }) {
  const [login, setLogin] = useState({ email: '', password: '' })
  const [register, setRegister] = useState(initialRegister)
  const [loadingCep, setLoadingCep] = useState(false)
  const [authMessage, setAuthMessage] = useState(null)
  const [authLoading, setAuthLoading] = useState(null)

  function changeMode(nextMode) {
    setMode(nextMode)
    setAuthMessage(null)
  }

  async function submitLogin(event) {
    event.preventDefault()
    setAuthMessage(null)

    if (!login.email.trim() || !login.password.trim()) {
      setAuthMessage({ type: 'error', text: 'Preencha e-mail e senha para acessar.' })
      return
    }

    try {
      const response = await api.post('/auth/login', login)
      setAuthLoading({ title: 'Validando acesso', detail: 'Conferindo suas credenciais com seguranca' })
      await wait(1500)
      setSession(response)
      setToast('Acesso autorizado.')
    } catch (error) {
      setAuthMessage({
        type: 'error',
        text: authErrorMessage(error, 'login'),
      })
    } finally {
      setAuthLoading(null)
    }
  }

  async function submitRegister(event) {
    event.preventDefault()
    setAuthMessage(null)

    if (!hasRequiredRegisterFields(register)) {
      setAuthMessage({ type: 'error', text: 'Preencha todos os campos obrigatorios antes de criar a conta.' })
      return
    }

    if (!isValidCpf(register.cpf)) {
      setAuthMessage({ type: 'error', text: 'CPF invalido. Confira os 11 digitos informados.' })
      return
    }

    if (digitsOnly(register.zipCode).length !== 8) {
      setAuthMessage({ type: 'error', text: 'CEP invalido. Use o formato 00000-000.' })
      return
    }

    try {
      const response = await api.post('/auth/register', {
        ...register,
        cpf: digitsOnly(register.cpf),
        zipCode: register.zipCode,
      })
      setAuthLoading({ title: 'Criando sua conta ACC', detail: 'Abrindo banco, perfil ecommerce e endereco de entrega' })
      await wait(1500)
      setSession(response)
      setToast('Cadastro criado com conta ACC Bank e perfil ecommerce.')
    } catch (error) {
      setAuthMessage({
        type: 'error',
        text: authErrorMessage(error, 'register'),
      })
    } finally {
      setAuthLoading(null)
    }
  }

  async function lookupCep() {
    const cep = register.zipCode.replace(/\D/g, '')
    if (cep.length !== 8) return
    setLoadingCep(true)
    try {
      const response = await fetch(`https://viacep.com.br/ws/${cep}/json/`)
      const data = await response.json()
      if (data.erro) {
        setAuthMessage({ type: 'error', text: 'CEP invalido ou nao encontrado no ViaCEP.' })
        return
      }
      setRegister((current) => ({
        ...current,
        zipCode: cep.replace(/(\d{5})(\d{3})/, '$1-$2'),
        state: data.uf || current.state,
        city: data.localidade || current.city,
        neighborhood: data.bairro || current.neighborhood,
        street: data.logradouro || current.street,
        complement: current.complement || data.complemento || '',
      }))
    } finally {
      setLoadingCep(false)
    }
  }

  return (
    <section className="auth-stage">
      {authLoading && <LoadingScreen title={authLoading.title} detail={authLoading.detail} compact />}
      <div className="auth-spotlight">
        <div className="floating-card card-one">
          <span>Saldo integrado</span>
          <strong>ACC Bank</strong>
        </div>
        <div className="floating-card card-two">
          <span>Checkout</span>
          <strong>Pix + Cartao</strong>
        </div>
        <p className="eyebrow">ACC Bank</p>
        <h2>Banco e ecommerce em uma experiencia so.</h2>
        <p>Entre para comprar, pagar, acompanhar pedidos e controlar sua conta.</p>
      </div>

      <div className={`auth-card ${mode === 'register' ? 'register-mode' : 'login-mode'}`}>
        <div className="auth-heading">
          <p className="eyebrow">{mode === 'login' ? 'Bem-vindo de volta' : 'Nova conta'}</p>
          <h2>{mode === 'login' ? 'Acesse sua ACC' : 'Crie sua conta integrada'}</h2>
        </div>

        <div className="segmented" aria-label="Alternar autenticacao">
          <button type="button" className={mode === 'login' ? 'active' : ''} onClick={() => changeMode('login')}>Entrar</button>
          <button type="button" className={mode === 'register' ? 'active' : ''} onClick={() => changeMode('register')}>Cadastrar</button>
          <span className="segmented-thumb" />
        </div>

        {authMessage && <div className={`auth-message ${authMessage.type}`}>{authMessage.text}</div>}

        <div className="form-slider" style={{ transform: mode === 'login' ? 'translateX(0)' : 'translateX(-50%)' }}>
          <div className="form-pane">
            <form onSubmit={submitLogin} className="stack-form">
              <label>E-mail<input value={login.email} onChange={(event) => setLogin({ ...login, email: event.target.value })} type="email" placeholder="voce@email.com" required /></label>
              <label>Senha<input value={login.password} onChange={(event) => setLogin({ ...login, password: event.target.value })} type="password" placeholder="Sua senha" required /></label>
              <button className="primary-button" type="submit">Entrar na ACC</button>
              <button className="link-button" type="button" onClick={() => changeMode('register')}>Ainda nao tenho conta</button>
              <div className="admin-hints">
                <small>Admin ecommerce: ecommerce.admin@accenture.com</small>
                <small>Admin banco: banking.admin@accenture.com</small>
                <small>Senha: admin123</small>
              </div>
            </form>
          </div>

          <div className="form-pane">
            <form onSubmit={submitRegister} className="stack-form compact">
              <label>Nome<input value={register.name} onChange={(event) => setRegister({ ...register, name: event.target.value })} placeholder="Nome completo" required /></label>
              <label>E-mail<input value={register.email} onChange={(event) => setRegister({ ...register, email: event.target.value })} type="email" placeholder="voce@email.com" required /></label>
              <label>Senha<input value={register.password} onChange={(event) => setRegister({ ...register, password: event.target.value })} type="password" minLength="8" placeholder="Minimo 8 caracteres" required /></label>
              <div className="two-col">
                <label>CPF<input value={register.cpf} onChange={(event) => setRegister({ ...register, cpf: formatCpf(event.target.value) })} inputMode="numeric" maxLength="14" placeholder="000.000.000-00" required /></label>
                <label>Nascimento<input value={register.birthDate} onChange={(event) => setRegister({ ...register, birthDate: event.target.value })} type="date" required /></label>
              </div>
              <div className="two-col">
                <label>Telefone<input value={register.phone} onChange={(event) => setRegister({ ...register, phone: event.target.value })} placeholder="(11) 99999-9999" required /></label>
                <label>CEP<input value={register.zipCode} onBlur={lookupCep} onChange={(event) => setRegister({ ...register, zipCode: formatCep(event.target.value) })} inputMode="numeric" maxLength="9" placeholder="00000-000" required /></label>
              </div>
              <small className="cep-helper">{loadingCep ? 'Consultando ViaCEP...' : 'Saia do campo CEP para preencher o endereco automaticamente.'}</small>
              <div className="two-col">
                <label>UF<input value={register.state} onChange={(event) => setRegister({ ...register, state: event.target.value.toUpperCase() })} maxLength="2" placeholder="SP" required /></label>
                <label>Cidade<input value={register.city} onChange={(event) => setRegister({ ...register, city: event.target.value })} placeholder="Sao Paulo" required /></label>
              </div>
              <label>Bairro<input value={register.neighborhood} onChange={(event) => setRegister({ ...register, neighborhood: event.target.value })} required /></label>
              <label>Rua<input value={register.street} onChange={(event) => setRegister({ ...register, street: event.target.value })} placeholder="Av. Paulista" required /></label>
              <div className="two-col">
                <label>Numero<input value={register.number} onChange={(event) => setRegister({ ...register, number: event.target.value })} placeholder="1000" required /></label>
                <label>Complemento<input value={register.complement} onChange={(event) => setRegister({ ...register, complement: event.target.value })} placeholder="Apto 1204" /></label>
              </div>
              <button className="primary-button" type="submit">Criar conta integrada</button>
              <button className="link-button" type="button" onClick={() => changeMode('login')}>Ja tenho cadastro</button>
            </form>
          </div>
        </div>
      </div>
    </section>
  )
}

function CustomerBank({ api }) {
  const [data, setData] = useState({})
  const [deposit, setDeposit] = useState({ amount: '', description: '' })
  const [pix, setPix] = useState('')
  const [invoicePay, setInvoicePay] = useState({ id: '', amount: '' })
  const [activeBankTab, setActiveBankTab] = useState('transactions')
  const [cardRevealed, setCardRevealed] = useState(false)

  const refresh = useCallback(async () => {
    const [account, balance, card, limit, purchases, invoices, currentInvoice, transactions] = await Promise.allSettled([
      api.get('/banking/accounts/me'),
      api.get('/banking/accounts/me/balance'),
      api.get('/banking/cards/me'),
      api.get('/banking/cards/me/limit'),
      api.get('/banking/cards/me/purchases'),
      api.get('/banking/invoices'),
      api.get('/banking/invoices/current'),
      api.get('/banking/accounts/me/transactions'),
    ])
    setData({
      account: settled(account),
      balance: settled(balance),
      card: settled(card),
      limit: settled(limit),
      purchases: settled(purchases, []),
      invoices: settled(invoices, []),
      currentInvoice: settled(currentInvoice),
      transactions: settled(transactions, []),
    })
  }, [api])

  useEffect(() => {
    refresh()
  }, [refresh])

  async function makeDeposit(event) {
    event.preventDefault()
    await api.post('/banking/accounts/me/deposit', { amount: Number(deposit.amount), description: deposit.description })
    setDeposit({ amount: '', description: '' })
    refresh()
  }

  async function payPix(event) {
    event.preventDefault()
    await api.get(`/banking/pix/${pix}`)
    await api.post('/banking/pix/pay', { code: pix })
    setPix('')
    refresh()
  }

  async function payInvoice(event) {
    event.preventDefault()
    await api.post(`/banking/invoices/${invoicePay.id}/pay`, { amount: Number(invoicePay.amount) })
    setInvoicePay({ id: '', amount: '' })
    refresh()
  }

  return (
    <div className="bank-dashboard">
      <section className="bank-hero nubank-style">
        <div className="bank-greeting">
          <p className="eyebrow">ACC Bank</p>
          <h2>Olá, sua vida financeira está aqui.</h2>
          <small>Conta {data.account?.accountNumber || '--'} - {data.account?.status || 'carregando'}</small>
        </div>

        <div className="balance-panel">
          <span>Saldo em conta</span>
          <strong>{money(data.balance?.balance ?? data.account?.balance)}</strong>
          <small>Disponivel para Pix, deposito e compras no ecommerce.</small>
        </div>
      </section>

      <section className="bank-shortcuts" aria-label="Acoes rapidas do banco">
        <button onClick={() => document.getElementById('deposit-action')?.focus()}>Depositar</button>
        <button onClick={() => document.getElementById('pix-action')?.focus()}>Pagar Pix</button>
        <button onClick={() => document.getElementById('invoice-action')?.focus()}>Pagar fatura</button>
        <button onClick={() => setActiveBankTab('transactions')}>Extrato</button>
      </section>

      <section className="bank-metrics">
        <BankMetric label="Limite disponivel" value={money(data.limit?.availableLimit)} helper={`Usado ${money(data.limit?.usedLimit)}`} progress={limitProgress(data.limit)} />
        <BankMetric label="Fatura atual" value={money(data.currentInvoice?.totalAmount)} helper={`${data.currentInvoice?.status || 'indisponivel'} - vence ${date(data.currentInvoice?.dueDate)}`} progress={invoiceProgress(data.currentInvoice)} />
        <BankMetric label="Lancamentos" value={data.transactions?.length || 0} helper="Movimentacoes recentes" progress={Math.min((data.transactions?.length || 0) * 12, 100)} />
      </section>

      <section className="bank-card-section">
        <button
          className={`credit-card-visual ${cardRevealed ? 'revealed' : ''}`}
          type="button"
          onClick={() => setCardRevealed((current) => !current)}
          aria-label={cardRevealed ? 'Ocultar dados do cartao' : 'Mostrar dados do cartao'}
        >
          <div className="card-face card-front">
            <div className="card-chip" />
            <span>ACC Platinum</span>
            <strong>{maskCard(data.card?.cardNumbers)}</strong>
            <div>
              <small>{data.card?.holderName || 'Cliente ACC'}</small>
              <small>{data.card?.expirationMonth ? `${String(data.card.expirationMonth).padStart(2, '0')}/${data.card.expirationYear}` : '--/--'}</small>
            </div>
          </div>
          <div className="card-face card-back">
            <span>Dados completos do cartao</span>
            <strong>{formatCardNumber(data.card?.cardNumbers)}</strong>
            <div className="card-secret-grid">
              <small>Titular <b>{data.card?.holderName || 'Cliente ACC'}</b></small>
              <small>CVV <b>{data.card?.cvv || '---'}</b></small>
              <small>Validade <b>{data.card?.expirationMonth ? `${String(data.card.expirationMonth).padStart(2, '0')}/${data.card.expirationYear}` : '--/--'}</b></small>
              <small>Status <b>{data.card?.status || '--'}</b></small>
              <small>Limite <b>{money(data.card?.creditLimit)}</b></small>
              <small>Disponivel <b>{money(data.card?.availableLimit)}</b></small>
            </div>
            <em>Clique novamente para ocultar</em>
          </div>
        </button>
        <div className="card-summary-panel">
          <span>Cartao de credito</span>
          <strong>{money(data.currentInvoice?.totalAmount)}</strong>
          <small>Fatura atual - limite disponivel {money(data.limit?.availableLimit)}</small>
          <button onClick={() => setActiveBankTab('purchases')}>Ver compras</button>
        </div>
      </section>

      <section className="bank-actions">
        <form onSubmit={makeDeposit} className="action-card">
          <div>
            <span>Deposito</span>
            <strong>Adicionar saldo</strong>
          </div>
          <input id="deposit-action" placeholder="Valor" value={deposit.amount} onChange={(event) => setDeposit({ ...deposit, amount: event.target.value })} type="number" step="0.01" required />
          <input placeholder="Descricao opcional" value={deposit.description} onChange={(event) => setDeposit({ ...deposit, description: event.target.value })} />
          <button>Depositar agora</button>
        </form>

        <form onSubmit={payPix} className="action-card">
          <div>
            <span>Pix ecommerce</span>
            <strong>Pagar pedido</strong>
          </div>
          <input id="pix-action" placeholder="Codigo Pix gerado na loja" value={pix} onChange={(event) => setPix(event.target.value)} required />
          <button>Pagar com saldo</button>
        </form>

        <form onSubmit={payInvoice} className="action-card">
          <div>
            <span>Cartao ACC</span>
            <strong>Pagar fatura</strong>
          </div>
          <input id="invoice-action" placeholder="ID da fatura" value={invoicePay.id} onChange={(event) => setInvoicePay({ ...invoicePay, id: event.target.value })} required />
          <input placeholder="Valor" value={invoicePay.amount} onChange={(event) => setInvoicePay({ ...invoicePay, amount: event.target.value })} type="number" step="0.01" required />
          <button>Quitar fatura</button>
        </form>
      </section>

      <section className="bank-activity panel">
        <div className="activity-header">
          <div>
            <p className="eyebrow">Historico financeiro</p>
            <h2>Movimentacoes da conta</h2>
          </div>
          <div className="pill-tabs">
            <button className={activeBankTab === 'transactions' ? 'active' : ''} onClick={() => setActiveBankTab('transactions')}>Transacoes</button>
            <button className={activeBankTab === 'purchases' ? 'active' : ''} onClick={() => setActiveBankTab('purchases')}>Compras</button>
            <button className={activeBankTab === 'invoices' ? 'active' : ''} onClick={() => setActiveBankTab('invoices')}>Faturas</button>
          </div>
        </div>

        {activeBankTab === 'transactions' && <Table rows={data.transactions} columns={['id', 'type', 'amount', 'balanceAfter', 'description', 'createdAt']} />}
        {activeBankTab === 'purchases' && <Table rows={data.purchases} columns={['id', 'invoiceId', 'amount', 'description', 'reference', 'purchaseDate']} />}
        {activeBankTab === 'invoices' && <Table rows={data.invoices} columns={['id', 'referenceMonth', 'totalAmount', 'paidAmount', 'status', 'dueDate']} />}
      </section>
    </div>
  )
}

function CustomerEcommerce({ api }) {
  const [catalog, setCatalog] = useState({ products: [], categories: [], cart: null, orders: [], cards: [], customer: null })
  const [filters, setFilters] = useState({ categoryName: '', maxPrice: '' })
  const [card, setCard] = useState({ cardNumber: '', cvv: '', expirationMonth: '', expirationYear: '', holderName: '' })
  const [checkoutCard, setCheckoutCard] = useState({ savedCardId: '', cvv: '' })
  const [profile, setProfile] = useState({ shippingAddress: '', phone: '' })
  const [selectedOrder, setSelectedOrder] = useState('')
  const [categoryLookup, setCategoryLookup] = useState('')
  const [productLookup, setProductLookup] = useState('')

  const refresh = useCallback(async () => {
    const query = new URLSearchParams()
    if (filters.categoryName) query.set('categoryName', filters.categoryName)
    if (filters.maxPrice) query.set('maxPrice', filters.maxPrice)
    const [products, categories, cart, orders, cards, customer] = await Promise.allSettled([
      api.get(`/ecommerce/products?${query.toString()}`),
      api.get('/ecommerce/categories'),
      api.get('/ecommerce/cart/me'),
      api.get('/ecommerce/orders'),
      api.get('/ecommerce/cards'),
      api.get('/customers/me'),
    ])
    const nextCustomer = settled(customer)
    setCatalog({
      products: settled(products)?.content || [],
      categories: settled(categories, []),
      cart: settled(cart),
      orders: settled(orders, []),
      cards: settled(cards, []),
      customer: nextCustomer,
    })
    if (nextCustomer) setProfile({ shippingAddress: nextCustomer.shippingAddress || '', phone: nextCustomer.phone || '' })
  }, [api, filters.categoryName, filters.maxPrice])

  useEffect(() => {
    refresh()
  }, [refresh])

  async function addToCart(productId) {
    await api.post('/ecommerce/cart/me/items', { productId, quantity: 1 })
    refresh()
  }

  async function updateCart(productId, quantity) {
    await api.put(`/ecommerce/cart/me/items/${productId}`, { quantity: Number(quantity) })
    refresh()
  }

  async function registerCard(event) {
    event.preventDefault()
    await api.post('/ecommerce/cards', {
      ...card,
      expirationMonth: Number(card.expirationMonth),
      expirationYear: Number(card.expirationYear),
    })
    setCard({ cardNumber: '', cvv: '', expirationMonth: '', expirationYear: '', holderName: '' })
    refresh()
  }

  async function checkoutPix() {
    const code = await api.post('/ecommerce/orders/checkout/pix')
    alert(`Codigo Pix: ${code}`)
    refresh()
  }

  async function payWithCard(event) {
    event.preventDefault()
    await api.post('/ecommerce/orders/checkout/card', { savedCardId: Number(checkoutCard.savedCardId), cvv: checkoutCard.cvv })
    setCheckoutCard({ savedCardId: '', cvv: '' })
    refresh()
  }

  async function updateProfile(event) {
    event.preventDefault()
    await api.put('/customers/me', profile)
    refresh()
  }

  async function lookupResources() {
    if (categoryLookup) await api.get(`/ecommerce/categories/${categoryLookup}`)
    if (productLookup) await api.get(`/ecommerce/products/${productLookup}`)
  }

  return (
    <div className="dashboard-grid ecommerce">
      <Panel title="Vitrine">
        <form className="inline-form" onSubmit={(event) => { event.preventDefault(); refresh() }}>
          <select value={filters.categoryName} onChange={(event) => setFilters({ ...filters, categoryName: event.target.value })}>
            <option value="">Todas categorias</option>
            {catalog.categories.map((category) => <option key={category.id} value={category.name}>{category.name}</option>)}
          </select>
          <input placeholder="Preco maximo" value={filters.maxPrice} onChange={(event) => setFilters({ ...filters, maxPrice: event.target.value })} type="number" />
          <button>Filtrar</button>
        </form>
        <div className="product-grid">
          {catalog.products.map((product) => (
            <article className="product-card product-card--visual" key={product.id}>
              <div className="product-card-cover">
                <img
                  src={placeholderImageForCategory(product.categoryName)}
                  alt=""
                  loading="lazy"
                  decoding="async"
                />
                <span className="product-card-badge">{product.categoryName}</span>
              </div>
              <h3>{product.name}</h3>
              <p>{product.description}</p>
              <strong>{money(product.price)}</strong>
              <button onClick={() => addToCart(product.id)}>Adicionar</button>
            </article>
          ))}
        </div>
      </Panel>

      <Panel title="Carrinho">
        <CartView cart={catalog.cart} onUpdate={updateCart} onRemove={(id) => api.delete(`/ecommerce/cart/me/items/${id}`).then(refresh)} />
        <div className="button-row">
          <button onClick={() => api.patch('/ecommerce/cart/close/me').then(refresh)}>Fechar carrinho</button>
          <button onClick={() => api.patch('/ecommerce/cart/open/me').then(refresh)}>Reabrir</button>
          <button onClick={() => api.delete('/ecommerce/cart/me').then(refresh)}>Limpar</button>
          <button onClick={checkoutPix}>Checkout Pix</button>
        </div>
        <form onSubmit={payWithCard} className="inline-form">
          <select value={checkoutCard.savedCardId} onChange={(event) => setCheckoutCard({ ...checkoutCard, savedCardId: event.target.value })} required>
            <option value="">Cartao salvo</option>
            {catalog.cards.map((saved) => <option key={saved.id} value={saved.id}>{saved.holderName} final {saved.last4Digits}</option>)}
          </select>
          <input placeholder="CVV" value={checkoutCard.cvv} onChange={(event) => setCheckoutCard({ ...checkoutCard, cvv: event.target.value })} required />
          <button>Pagar com cartao</button>
        </form>
      </Panel>

      <Panel title="Cartoes salvos">
        <form onSubmit={registerCard} className="stack-form compact wide">
          <input placeholder="Numero do cartao" value={card.cardNumber} onChange={(event) => setCard({ ...card, cardNumber: event.target.value })} required />
          <div className="three-col">
            <input placeholder="CVV" value={card.cvv} onChange={(event) => setCard({ ...card, cvv: event.target.value })} required />
            <input placeholder="Mes" value={card.expirationMonth} onChange={(event) => setCard({ ...card, expirationMonth: event.target.value })} required />
            <input placeholder="Ano" value={card.expirationYear} onChange={(event) => setCard({ ...card, expirationYear: event.target.value })} required />
          </div>
          <input placeholder="Nome impresso" value={card.holderName} onChange={(event) => setCard({ ...card, holderName: event.target.value })} />
          <button>Cadastrar cartao</button>
        </form>
        <List items={catalog.cards} render={(saved) => `${saved.holderName} - final ${saved.last4Digits}`} action={(saved) => <button onClick={() => api.delete(`/ecommerce/cards/${saved.id}`).then(refresh)}>Excluir</button>} />
      </Panel>

      <Panel title="Perfil ecommerce">
        <form onSubmit={updateProfile} className="stack-form compact wide">
          <textarea placeholder="Endereco de entrega" value={profile.shippingAddress} onChange={(event) => setProfile({ ...profile, shippingAddress: event.target.value })} required />
          <input placeholder="Telefone" value={profile.phone} onChange={(event) => setProfile({ ...profile, phone: event.target.value })} required />
          <button>Atualizar perfil</button>
        </form>
        <small>Tier atual: {catalog.customer?.tier || 'indisponivel'} - compras: {catalog.customer?.quantityPurchases ?? 0}</small>
      </Panel>

      <Panel title="Consultas diretas">
        <form className="inline-form" onSubmit={(event) => { event.preventDefault(); lookupResources() }}>
          <input placeholder="Categoria por identificador" value={categoryLookup} onChange={(event) => setCategoryLookup(event.target.value)} />
          <input placeholder="Produto por ID" value={productLookup} onChange={(event) => setProductLookup(event.target.value)} />
          <button>Consultar</button>
        </form>
      </Panel>

      <TablePanel title="Pedidos" rows={catalog.orders} columns={['orderId', 'status', 'paymentMethod', 'subtotal', 'discountTotal', 'totalAmount', 'createdAt']} action={(order) => (
        <button onClick={() => api.get(`/ecommerce/orders/${order.orderId}`).then(() => setSelectedOrder(order.orderId))}>Ver</button>
      )} />
      {selectedOrder && <button className="danger-button" onClick={() => api.post(`/ecommerce/orders/${selectedOrder}/cancel`).then(refresh)}>Cancelar pedido {selectedOrder}</button>}
    </div>
  )
}

function AdminEcommerce({ api }) {
  const [categories, setCategories] = useState([])
  const [category, setCategory] = useState({ id: '', name: '', description: '' })
  const [product, setProduct] = useState({ id: '', name: '', description: '', price: '', initialStock: '', categoryName: '' })
  const [restock, setRestock] = useState({ id: '', quantity: '' })
  const [emails, setEmails] = useState([])
  const [emailFilter, setEmailFilter] = useState({ status: '', type: '', id: '' })

  const refresh = useCallback(async () => {
    const [categoryList, emailPage] = await Promise.allSettled([
      api.get('/ecommerce/categories'),
      api.get('/admin/notifications/emails'),
    ])
    setCategories(settled(categoryList, []))
    setEmails(settled(emailPage)?.content || [])
  }, [api])

  useEffect(() => {
    refresh()
  }, [refresh])

  async function saveCategory(event) {
    event.preventDefault()
    const body = { name: category.name, description: category.description }
    if (category.id) await api.put(`/ecommerce/admin/categories/${category.id}`, body)
    else await api.post('/ecommerce/admin/categories', body)
    setCategory({ id: '', name: '', description: '' })
    refresh()
  }

  async function saveProduct(event) {
    event.preventDefault()
    const body = {
      name: product.name,
      description: product.description,
      price: Number(product.price),
      initialStock: Number(product.initialStock || 0),
      categoryName: product.categoryName,
    }
    if (product.id) await api.put(`/ecommerce/admin/products/${product.id}`, body)
    else await api.post('/ecommerce/admin/products', body)
    setProduct({ id: '', name: '', description: '', price: '', initialStock: '', categoryName: '' })
  }

  async function filterEmails(event) {
    event.preventDefault()
    if (emailFilter.id) {
      const single = await api.get(`/admin/notifications/emails/${emailFilter.id}`)
      setEmails([single])
      return
    }
    const query = new URLSearchParams()
    if (emailFilter.status) query.set('status', emailFilter.status)
    if (emailFilter.type) query.set('type', emailFilter.type)
    const page = await api.get(`/admin/notifications/emails?${query.toString()}`)
    setEmails(page.content || [])
  }

  return (
    <div className="dashboard-grid admin">
      <Panel title="Categorias">
        <form onSubmit={saveCategory} className="inline-form">
          <input placeholder="ID para editar" value={category.id} onChange={(event) => setCategory({ ...category, id: event.target.value })} />
          <input placeholder="Nome" value={category.name} onChange={(event) => setCategory({ ...category, name: event.target.value })} required />
          <input placeholder="Descricao" value={category.description} onChange={(event) => setCategory({ ...category, description: event.target.value })} />
          <button>Salvar</button>
        </form>
        <List items={categories} render={(item) => `${item.id} - ${item.name}`} action={(item) => <button onClick={() => api.delete(`/ecommerce/admin/categories/${item.id}`).then(refresh)}>Excluir</button>} />
      </Panel>

      <Panel title="Produtos">
        <form onSubmit={saveProduct} className="stack-form compact wide">
          <input placeholder="ID para editar" value={product.id} onChange={(event) => setProduct({ ...product, id: event.target.value })} />
          <input placeholder="Nome" value={product.name} onChange={(event) => setProduct({ ...product, name: event.target.value })} required />
          <textarea placeholder="Descricao" value={product.description} onChange={(event) => setProduct({ ...product, description: event.target.value })} />
          <div className="three-col">
            <input placeholder="Preco" value={product.price} onChange={(event) => setProduct({ ...product, price: event.target.value })} type="number" step="0.01" required />
            <input placeholder="Estoque inicial" value={product.initialStock} onChange={(event) => setProduct({ ...product, initialStock: event.target.value })} type="number" />
            <input placeholder="Categoria" value={product.categoryName} onChange={(event) => setProduct({ ...product, categoryName: event.target.value })} required />
          </div>
          <button>Salvar produto</button>
        </form>
        <form className="inline-form" onSubmit={(event) => { event.preventDefault(); api.post(`/ecommerce/admin/products/${restock.id}/restock?quantity=${restock.quantity}`).then(() => setRestock({ id: '', quantity: '' })) }}>
          <input placeholder="ID produto" value={restock.id} onChange={(event) => setRestock({ ...restock, id: event.target.value })} required />
          <input placeholder="Quantidade" value={restock.quantity} onChange={(event) => setRestock({ ...restock, quantity: event.target.value })} type="number" required />
          <button>Repor estoque</button>
        </form>
        <form className="inline-form" onSubmit={(event) => { event.preventDefault(); api.post(`/ecommerce/admin/products/${product.id}/deactivate`).then(() => setProduct({ ...product, id: '' })) }}>
          <input placeholder="ID produto para desativar" value={product.id} onChange={(event) => setProduct({ ...product, id: event.target.value })} required />
          <button>Desativar</button>
        </form>
      </Panel>

      <Panel title="Emails transacionais">
        <form onSubmit={filterEmails} className="inline-form">
          <select value={emailFilter.status} onChange={(event) => setEmailFilter({ ...emailFilter, status: event.target.value })}>
            <option value="">Todos status</option>
            <option value="PENDING">PENDING</option>
            <option value="SENT">SENT</option>
            <option value="FAILED">FAILED</option>
          </select>
          <input placeholder="Tipo" value={emailFilter.type} onChange={(event) => setEmailFilter({ ...emailFilter, type: event.target.value })} />
          <input placeholder="ID email" value={emailFilter.id} onChange={(event) => setEmailFilter({ ...emailFilter, id: event.target.value })} />
          <button>Buscar</button>
        </form>
        <Table rows={emails} columns={['id', 'toEmail', 'subject', 'type', 'status', 'createdAt']} />
      </Panel>
    </div>
  )
}

function AdminBank({ api }) {
  const [accounts, setAccounts] = useState([])
  const [transactions, setTransactions] = useState([])
  const [deposit, setDeposit] = useState({ id: '', amount: '', description: '' })
  const [invoiceId, setInvoiceId] = useState('')
  const [emails, setEmails] = useState([])

  const refresh = useCallback(async () => {
    const [accountList, transactionList, emailPage] = await Promise.allSettled([
      api.get('/banking/admin'),
      api.get('/banking/admin/accounts/transactions'),
      api.get('/admin/notifications/emails'),
    ])
    setAccounts(settled(accountList, []))
    setTransactions(settled(transactionList, []))
    setEmails(settled(emailPage)?.content || [])
  }, [api])

  useEffect(() => {
    refresh()
  }, [refresh])

  async function adminDeposit(event) {
    event.preventDefault()
    await api.post(`/banking/admin/accounts/${deposit.id}/deposit`, { amount: Number(deposit.amount), description: deposit.description })
    setDeposit({ id: '', amount: '', description: '' })
    refresh()
  }

  return (
    <div className="dashboard-grid admin">
      <Panel title="Operacoes bancarias">
        <form onSubmit={adminDeposit} className="inline-form">
          <input placeholder="ID conta" value={deposit.id} onChange={(event) => setDeposit({ ...deposit, id: event.target.value })} required />
          <input placeholder="Valor" value={deposit.amount} onChange={(event) => setDeposit({ ...deposit, amount: event.target.value })} type="number" step="0.01" required />
          <input placeholder="Descricao" value={deposit.description} onChange={(event) => setDeposit({ ...deposit, description: event.target.value })} />
          <button>Depositar</button>
        </form>
        <form className="inline-form" onSubmit={(event) => { event.preventDefault(); api.post(`/banking/admin/billing/invoices/${invoiceId}/close`).then(() => setInvoiceId('')) }}>
          <input placeholder="ID fatura" value={invoiceId} onChange={(event) => setInvoiceId(event.target.value)} required />
          <button>Fechar fatura</button>
        </form>
        <div className="button-row">
          <button onClick={() => api.post('/banking/admin/billing/charge-overdue')}>Cobrar vencidas</button>
          <button onClick={() => api.post('/banking/admin/billing/run-day')}>Rodar ciclo diario</button>
        </div>
      </Panel>

      <TablePanel title="Contas" rows={accounts} columns={['id', 'userId', 'accountNumber', 'balance', 'accountType', 'status', 'createdAt']} action={(account) => (
        <div className="button-row tight">
          <button onClick={() => api.post(`/banking/admin/accounts/${account.id}/block`).then(refresh)}>Bloquear</button>
          <button onClick={() => api.post(`/banking/admin/accounts/${account.id}/unblock`).then(refresh)}>Desbloquear</button>
        </div>
      )} />
      <TablePanel title="Todas transacoes" rows={transactions} columns={['id', 'type', 'amount', 'balanceAfter', 'reference', 'description', 'createdAt']} />
      <TablePanel title="Emails do banco" rows={emails} columns={['id', 'toEmail', 'subject', 'type', 'status', 'createdAt']} />
    </div>
  )
}

function CustomerHome({ api, setActiveView, setToast }) {
  const [feedLoading, setFeedLoading] = useState(true)
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [cart, setCart] = useState(null)
  const [categoryFilter, setCategoryFilter] = useState('')

  const refreshFeed = useCallback(async () => {
    setFeedLoading(true)
    const query = new URLSearchParams({ size: '24', sort: 'id,desc' })
    if (categoryFilter) query.set('categoryName', categoryFilter)
    const [productsRes, categoriesRes, cartRes] = await Promise.allSettled([
      api.get(`/ecommerce/products?${query.toString()}`),
      api.get('/ecommerce/categories'),
      api.get('/ecommerce/cart/me'),
    ])
    setProducts(settled(productsRes)?.content || [])
    setCategories(settled(categoriesRes, []))
    setCart(settled(cartRes))
    setFeedLoading(false)
  }, [api, categoryFilter])

  useEffect(() => {
    refreshFeed()
  }, [refreshFeed])

  async function addFromFeed(productId) {
    try {
      await api.post('/ecommerce/cart/me/items', { productId, quantity: 1 })
      setToast('Adicionado ao carrinho.')
      refreshFeed()
    } catch {
      /* createApi ja notifica */
    }
  }

  const cartCount = cart?.items?.reduce((acc, item) => acc + (item.quantity || 0), 0) || 0

  return (
    <section className="profile-home customer-home customer-feed">
      <div className="hero-panel feed-hero">
        <div>
          <p className="eyebrow">Feed da loja</p>
          <h2>Descubra ofertas com imagens por categoria.</h2>
          <p>
            Como o catalogo nao envia fotos do servidor, mostramos uma imagem generica por tipo de categoria (livros,
            comida, eletronicos e outras). Sem alterar o backend.
          </p>
          <div className="button-row">
            <button type="button" onClick={() => setActiveView('customerEcommerce')}>
              Abrir loja completa
            </button>
            <button type="button" className="ghost-button light-ghost" onClick={() => setActiveView('customerBank')}>
              Ir ao banco
            </button>
          </div>
        </div>
        <aside className="mini-cart" aria-label="Resumo do carrinho">
          <div className="mini-cart-header">
            <strong>Seu carrinho</strong>
            <span>{cartCount} itens</span>
          </div>
          {cart?.items?.length ? (
            <ul className="mini-cart-lines">
              {cart.items.slice(0, 4).map((item) => (
                <li key={item.productId}>
                  <span>{item.productName}</span>
                  <small>
                    {item.quantity}x {money(item.unitPrice)}
                  </small>
                </li>
              ))}
            </ul>
          ) : (
            <p className="mini-cart-empty">Nada por aqui ainda. Adicione pelo feed.</p>
          )}
          <div className="mini-cart-footer">
            <span>Subtotal</span>
            <strong>{money(cart?.subtotal)}</strong>
          </div>
          <button type="button" className="primary-button block" onClick={() => setActiveView('customerEcommerce')}>
            Ver carrinho e checkout
          </button>
        </aside>
      </div>

      <div className="feed-toolbar">
        <p className="eyebrow">Categorias</p>
        <div className="feed-chips">
          <button type="button" className={categoryFilter === '' ? 'active' : ''} onClick={() => setCategoryFilter('')}>
            Todas
          </button>
          {categories.map((c) => (
            <button
              type="button"
              key={c.id}
              className={categoryFilter === c.name ? 'active' : ''}
              onClick={() => setCategoryFilter(c.name)}
            >
              {c.name}
            </button>
          ))}
        </div>
      </div>

      {feedLoading ? (
        <p className="empty-state">Carregando vitrine...</p>
      ) : (
        <div className="feed-grid">
          {products.map((product) => (
            <article className="feed-card" key={product.id}>
              <div className="feed-card-cover">
                <img
                  src={placeholderImageForCategory(product.categoryName)}
                  alt=""
                  loading="lazy"
                  decoding="async"
                />
                <span className="feed-card-chip">{product.categoryName}</span>
              </div>
              <div className="feed-card-body">
                <h3>{product.name}</h3>
                <p>{product.description}</p>
                <div className="feed-card-meta">
                  <strong>{money(product.price)}</strong>
                  <small>Estoque {product.availableStock}</small>
                </div>
                <button type="button" onClick={() => addFromFeed(product.id)}>
                  Adicionar ao carrinho
                </button>
              </div>
            </article>
          ))}
        </div>
      )}

      {!feedLoading && !products.length ? <p className="empty-state">Nenhum produto neste filtro.</p> : null}

      <div className="journey-grid feed-journey">
        <article>
          <strong>1. Comprar</strong>
          <span>Monte o carrinho pelo feed ou pela loja.</span>
        </article>
        <article>
          <strong>2. Pagar</strong>
          <span>Pix ou cartao salvo na area da loja.</span>
        </article>
        <article>
          <strong>3. Acompanhar</strong>
          <span>Pedidos e banco na mesma conta.</span>
        </article>
      </div>
    </section>
  )
}

function AdminHome({ roles, setActiveView }) {
  return (
    <section className="profile-home admin-home">
      <div className="hero-panel">
        <p className="eyebrow">Administracao ACC</p>
        <h2>A interface agora muda conforme o perfil autenticado.</h2>
        <p>Admins veem apenas as operacoes administrativas permitidas pelo papel retornado no login. Clientes nao recebem estes menus.</p>
        <div className="button-row">
          {roles.includes('ECOMMERCE_ADMIN') && <button onClick={() => setActiveView('adminEcommerce')}>Gerenciar ecommerce</button>}
          {roles.includes('BANKING_ADMIN') && <button onClick={() => setActiveView('adminBank')}>Gerenciar banco</button>}
        </div>
      </div>
      <div className="journey-grid">
        {roles.includes('ECOMMERCE_ADMIN') && (
          <article>
            <strong>Ecommerce</strong>
            <span>Categorias, produtos, estoque e emails transacionais.</span>
          </article>
        )}
        {roles.includes('BANKING_ADMIN') && (
          <article>
            <strong>Banco</strong>
            <span>Contas, bloqueios, depositos, transacoes e faturamento.</span>
          </article>
        )}
        <article>
          <strong>Perfis separados</strong>
          <span>CUSTOMER opera a experiencia de compra e banco; ADMIN opera gestao.</span>
        </article>
      </div>
    </section>
  )
}

function Panel({ title, children }) {
  return (
    <section className="panel">
      <h2>{title}</h2>
      {children}
    </section>
  )
}

function BankMetric({ label, value, helper, progress }) {
  return (
    <article className="bank-metric">
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{helper}</small>
      <div className="metric-track">
        <i style={{ width: `${Math.max(0, Math.min(progress || 0, 100))}%` }} />
      </div>
    </article>
  )
}

function TablePanel({ title, rows = [], columns = [], action }) {
  return (
    <Panel title={title}>
      <Table rows={rows} columns={columns} action={action} />
    </Panel>
  )
}

function Table({ rows = [], columns = [], action }) {
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

function CartView({ cart, onUpdate, onRemove }) {
  if (!cart?.items?.length) return <p className="empty-state">Carrinho vazio.</p>
  return (
    <div className="cart-list">
      {cart.items.map((item) => (
        <div className="cart-row" key={item.productId}>
          <div>
            <strong>{item.productName}</strong>
            <small>{money(item.unitPrice)} cada</small>
          </div>
          <input value={item.quantity} min="0" type="number" onChange={(event) => onUpdate(item.productId, event.target.value)} />
          <strong>{money(item.lineTotal)}</strong>
          <button onClick={() => onRemove(item.productId)}>Remover</button>
        </div>
      ))}
      <div className="cart-total">Subtotal {money(cart.subtotal)}</div>
    </div>
  )
}

function List({ items = [], render, action }) {
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

function settled(result, fallback = null) {
  return result.status === 'fulfilled' ? result.value : fallback
}

function viewTitle(view) {
  const titles = {
    customerHome: 'Feed principal',
    customerBank: 'Banco',
    customerEcommerce: 'Loja',
    adminHome: 'Painel admin',
    adminEcommerce: 'Gestao ecommerce',
    adminBank: 'Gestao banco',
  }
  return titles[view]
}

function defaultViewForRoles(roles) {
  if (isAdmin(roles)) return 'adminHome'
  return 'customerHome'
}

function isAdmin(roles) {
  return roles.includes('BANKING_ADMIN') || roles.includes('ECOMMERCE_ADMIN')
}

function digitsOnly(value) {
  return String(value || '').replace(/\D/g, '')
}

function formatCpf(value) {
  return digitsOnly(value)
    .slice(0, 11)
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d{1,2})$/, '$1-$2')
}

function formatCep(value) {
  return digitsOnly(value)
    .slice(0, 8)
    .replace(/(\d{5})(\d)/, '$1-$2')
}

function isValidCpf(value) {
  const cpf = digitsOnly(value)
  if (cpf.length !== 11 || /^(\d)\1+$/.test(cpf)) return false

  const calculateDigit = (base) => {
    const sum = base
      .split('')
      .reduce((total, digit, index) => total + Number(digit) * (base.length + 1 - index), 0)
    const rest = (sum * 10) % 11
    return rest === 10 ? 0 : rest
  }

  const firstDigit = calculateDigit(cpf.slice(0, 9))
  const secondDigit = calculateDigit(cpf.slice(0, 10))
  return firstDigit === Number(cpf[9]) && secondDigit === Number(cpf[10])
}

function hasRequiredRegisterFields(values) {
  const requiredFields = [
    'name',
    'email',
    'password',
    'cpf',
    'birthDate',
    'phone',
    'zipCode',
    'state',
    'city',
    'neighborhood',
    'street',
    'number',
  ]

  return requiredFields.every((field) => String(values[field] || '').trim())
}

function authErrorMessage(error, context) {
  const rawMessage = `${error?.message || ''}`.toLowerCase()
  const fields = error?.payload?.fields || {}

  if (fields.cpf) return 'CPF invalido. Confira os dados e tente novamente.'
  if (fields.zipCode) return 'CEP invalido. Use o formato 00000-000.'
  if (fields.email) return fields.email

  if (rawMessage.includes('senha') || rawMessage.includes('credential') || error?.status === 401) {
    return 'E-mail ou senha incorretos. Confira os dados e tente novamente.'
  }
  if (rawMessage.includes('e-mail') || rawMessage.includes('email')) {
    return 'Este e-mail ja esta cadastrado. Use outro e-mail ou faca login.'
  }
  if (rawMessage.includes('cpf')) {
    return rawMessage.includes('cadastrado')
      ? 'Este CPF ja esta cadastrado. Use outro CPF ou faca login.'
      : 'CPF invalido. Confira os dados e tente novamente.'
  }
  if (rawMessage.includes('cep') || rawMessage.includes('zipcode')) {
    return 'CEP invalido. Use o formato 00000-000.'
  }

  return context === 'login'
    ? 'Nao foi possivel entrar. Confira e-mail e senha.'
    : 'Nao foi possivel criar sua conta. Confira os dados informados.'
}

function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function maskCard(value) {
  const cardDigits = digitsOnly(value)
  if (!cardDigits) return '**** **** **** ----'
  return cardDigits.replace(/\d(?=\d{4})/g, '*').replace(/(.{4})/g, '$1 ').trim()
}

function formatCardNumber(value) {
  const cardDigits = digitsOnly(value)
  if (!cardDigits) return '---- ---- ---- ----'
  return cardDigits.replace(/(.{4})/g, '$1 ').trim()
}

function limitProgress(limit) {
  const total = Number(limit?.creditLimit || 0)
  if (!total) return 0
  return (Number(limit?.usedLimit || 0) / total) * 100
}

function invoiceProgress(invoice) {
  const total = Number(invoice?.totalAmount || 0)
  if (!total) return 0
  return (Number(invoice?.paidAmount || 0) / total) * 100
}

function money(value) {
  const numeric = Number(value || 0)
  return numeric.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function date(value) {
  if (!value) return '--'
  return new Date(value).toLocaleDateString('pt-BR')
}

function formatCell(value) {
  if (value === null || value === undefined) return '--'
  if (Array.isArray(value)) return `${value.length} itens`
  if (typeof value === 'object') return JSON.stringify(value)
  if (typeof value === 'number') return value.toLocaleString('pt-BR')
  if (typeof value === 'string' && value.match(/^\d{4}-\d{2}-\d{2}T/)) return new Date(value).toLocaleString('pt-BR')
  return value
}

export default App
