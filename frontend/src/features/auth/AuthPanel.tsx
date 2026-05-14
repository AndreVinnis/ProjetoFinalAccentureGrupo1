/* eslint-disable @typescript-eslint/ban-ts-comment */
// @ts-nocheck
import { useState } from 'react'
import { LoadingScreen } from '../../components/layout/LoadingScreen'
import { authErrorMessage, hasRequiredRegisterFields, initialRegister, isValidCpf, wait } from '../../utils/auth'
import { digitsOnly, formatCep, formatCpf } from '../../utils/format'

export function AuthPanel({ api, mode, setMode, setSession, setToast }) {
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
      setAuthMessage({ type: 'error', text: authErrorMessage(error, 'login') })
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
      setAuthMessage({ type: 'error', text: authErrorMessage(error, 'register') })
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
