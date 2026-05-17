import '../../../styles/ecommerce/profile.css'
import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Panel } from '../../../components/ui/Panel'
import { List } from '../../../components/ui/List'
import { settled } from '../../../utils/async'
import { date, digitsOnly, formatCardNumber } from '../../../utils/format'
import type { ApiClient } from '../../../services/api'
import type { Order } from '../types/order'
import type { SavedCard } from '../types/savedCard'
import type { Customer } from '../types/customer'

function sessionName() {
  try {
    const session = JSON.parse(localStorage.getItem('acc_session') || 'null')
    return session?.name || 'Cliente ACC'
  } catch {
    return 'Cliente ACC'
  }
}

function addressParts(address: string) {
  if (!address) return []
  return address.split(',').map((part) => part.trim()).filter(Boolean)
}

export function Profile({ api }: { api: ApiClient }) {
  const navigate = useNavigate()

  const [cards, setCards] = useState<SavedCard[]>([])
  const [customer, setCustomer] = useState<Customer | null>(null)
  
  // Perfil exibido na visualização
  const [profile, setProfile] = useState({ shippingAddress: '', phone: '' })
  
  // Estado exclusivo para o formulário de edição
  const [profileForm, setProfileForm] = useState({
    phone: '',
    zipCode: '',
    street: '',
    number: '',
    complement: '',
    neighborhood: '',
    city: '',
    state: ''
  })

  const [card, setCard] = useState({ cardNumber: '', cvv: '', expirationMonth: '', expirationYear: '', holderName: '' })
  
  const [isEditingProfile, setIsEditingProfile] = useState(false)
  const [loadingCep, setLoadingCep] = useState(false)
  const [cepError, setCepError] = useState('')
  const [savingCard, setSavingCard] = useState(false)
  const [cardSaved, setCardSaved] = useState(false)

  const customerName = sessionName()
  const splitAddress = addressParts(profile.shippingAddress)
  const cardPreviewNumber = formatCardNumber(card.cardNumber).padEnd(19, '-')
  const cardPreviewHolder = card.holderName || customerName
  const cardPreviewExpiry = card.expirationMonth || card.expirationYear ? `${String(card.expirationMonth || '--').padStart(2, '0')}/${card.expirationYear || '----'}` : '--/----'

  const refresh = useCallback(async () => {
    const [ordersResult, cardsResult, customerResult] = await Promise.allSettled([
      api.get<Order[]>('/ecommerce/orders'),
      api.get<SavedCard[]>('/ecommerce/cards'),
      api.get<Customer>('/customers/me'),
    ])
    
    const nextCustomer = settled(customerResult)

    return {
      orders: settled(ordersResult, []) ?? [],
      cards: settled(cardsResult, []) ?? [],
      customer: nextCustomer ?? null,
      profile: nextCustomer ? { shippingAddress: nextCustomer.shippingAddress || '', phone: nextCustomer.phone || '' } : null
    }
  }, [api])

  useEffect(() => {
    async function load() {
      const data = await refresh()
      
      setCards(data.cards)
      setCustomer(data.customer)
      if (data.profile) setProfile(data.profile)
    }
    
    void load()
  }, [refresh])

  // Função do ViaCEP adaptada para o estado do perfil
  async function lookupCep() {
    const cep = profileForm.zipCode.replace(/\D/g, '')
    if (cep.length !== 8) return
    
    setLoadingCep(true)
    setCepError('')
    
    try {
      const response = await fetch(`https://viacep.com.br/ws/${cep}/json/`)
      const data = await response.json()
      
      if (data.erro) {
        setCepError('CEP inválido ou não encontrado no ViaCEP.')
        return
      }
      
      setProfileForm((current) => ({
        ...current,
        zipCode: cep.replace(/(\d{5})(\d{3})/, '$1-$2'),
        state: data.uf || current.state,
        city: data.localidade || current.city,
        neighborhood: data.bairro || current.neighborhood,
        street: data.logradouro || current.street,
        complement: current.complement || data.complemento || '',
      }))
    } catch {
      setCepError('Erro ao buscar o CEP.')
    } finally {
      setLoadingCep(false)
    }
  }

  function handleEditClick() {
    const [street = '', numberComplement = '', neighborhood = '', cityState = '', zipCode = ''] = splitAddress
    const [number = '', complement = ''] = numberComplement.split(' - ').map((part) => part.trim())
    const [city = '', state = ''] = cityState.split(' - ').map((part) => part.trim())

    setProfileForm(current => ({
      ...current,
      phone: profile.phone,
      street: street || current.street,
      number: number || current.number,
      complement: complement || current.complement,
      neighborhood: neighborhood || current.neighborhood,
      city: city || current.city,
      state: state || current.state,
      zipCode: zipCode || current.zipCode,
    }))
    setCepError('')
    setIsEditingProfile(true)
  }

  async function updateProfile(event: React.FormEvent) {
    event.preventDefault()
    
    // Constrói o endereço completo em uma única string para salvar no backend
    const fullAddress = `${profileForm.street}, ${profileForm.number}${profileForm.complement ? ' - ' + profileForm.complement : ''}, ${profileForm.neighborhood}, ${profileForm.city} - ${profileForm.state}, ${profileForm.zipCode}`

    const payload = {
      shippingAddress: fullAddress,
      phone: profileForm.phone
    }

    await api.put('/customers/me', payload)
    
    const data = await refresh()
    
    setCards(data.cards)
    setCustomer(data.customer)
    if (data.profile) setProfile(data.profile)
    
    setIsEditingProfile(false)
  }

  async function registerCard(event: React.FormEvent) {
    event.preventDefault()
    setSavingCard(true)
    setCardSaved(false)
    
    try {
      await api.post('/ecommerce/cards', { 
        ...card, 
        expirationMonth: Number(card.expirationMonth), 
        expirationYear: Number(card.expirationYear) 
      })
      
      setCard({ cardNumber: '', cvv: '', expirationMonth: '', expirationYear: '', holderName: '' })
      
      const data = await refresh()
      
      setCards(data.cards)
      setCustomer(data.customer)
      if (data.profile) setProfile(data.profile)
      setCardSaved(true)
      window.setTimeout(() => setCardSaved(false), 1800)
    } finally {
      setSavingCard(false)
    }
  }

  async function removeCard(id: number) {
    await api.delete(`/ecommerce/cards/${id}`)
    
    const data = await refresh()
    
    setCards(data.cards)
    setCustomer(data.customer)
    if (data.profile) setProfile(data.profile)
  }

  return (
    <div className="dashboard-grid ecommerce account-workspace">
      <Panel title="Informações do cliente">
        <div className="profile-identity-card">
          <div className="profile-avatar" aria-hidden="true">{customerName.charAt(0).toUpperCase()}</div>
          <div>
            <span>Minha conta</span>
            <strong>{customerName}</strong>
            <small>Cliente desde {date(customer?.createdAt)}</small>
          </div>
        </div>

        <div className="profile-stats-grid">
          <div>
            <span>Tier</span>
            <strong>{customer?.tier || 'indisponivel'}</strong>
          </div>
          <div>
            <span>Compras</span>
            <strong>{customer?.quantityPurchases ?? 0}</strong>
          </div>
          <div>
            <span>Telefone</span>
            <strong>{profile.phone || '--'}</strong>
          </div>
        </div>
        {!isEditingProfile ? (
          <div className="profile-view-mode">
            <div className="profile-field">
              <strong>Endereço de entrega</strong>
              {splitAddress.length ? (
                <div className="profile-address-card">
                  {splitAddress.map((part, index) => (
                    <span key={`${part}-${index}`}>{part}</span>
                  ))}
                </div>
              ) : (
                <p>Nenhum endereço cadastrado.</p>
              )}
            </div>
            <div className="profile-form-actions">
              <button onClick={handleEditClick}>
                Editar Informações
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={updateProfile} className="stack-form compact wide" style={{ marginBottom: '1rem' }}>
            <div className="two-col">
              <input 
                placeholder="Telefone" 
                value={profileForm.phone} 
                onChange={(e) => setProfileForm({ ...profileForm, phone: e.target.value })} 
                required 
              />
              <input 
                placeholder="CEP" 
                value={profileForm.zipCode} 
                onChange={(e) => {
                  let val = e.target.value.replace(/\D/g, '')
                  if (val.length > 5) val = val.replace(/^(\d{5})(\d)/, '$1-$2')
                  setProfileForm({ ...profileForm, zipCode: val })
                }}
                onBlur={lookupCep}
                maxLength={9}
                required 
              />
            </div>
            
            {cepError && <div className="field-error-message" style={{ color: '#ff9a9a', fontSize: '12px', marginTop: '-4px' }}>{cepError}</div>}

            <div className="two-col">
              <input 
                placeholder="Rua / Logradouro" 
                value={profileForm.street} 
                onChange={(e) => setProfileForm({ ...profileForm, street: e.target.value })} 
                disabled={loadingCep}
                required 
              />
              <div className="two-col">
                <input 
                  placeholder="Número" 
                  value={profileForm.number} 
                  onChange={(e) => setProfileForm({ ...profileForm, number: e.target.value })} 
                  disabled={loadingCep}
                  required 
                />
                <input 
                  placeholder="Complemento" 
                  value={profileForm.complement} 
                  onChange={(e) => setProfileForm({ ...profileForm, complement: e.target.value })} 
                  disabled={loadingCep}
                />
              </div>
            </div>

            <div className="three-col">
              <input 
                placeholder="Bairro" 
                value={profileForm.neighborhood} 
                onChange={(e) => setProfileForm({ ...profileForm, neighborhood: e.target.value })} 
                disabled={loadingCep}
                required 
              />
              <input 
                placeholder="Cidade" 
                value={profileForm.city} 
                onChange={(e) => setProfileForm({ ...profileForm, city: e.target.value })} 
                disabled={loadingCep}
                required 
              />
              <input 
                placeholder="UF" 
                value={profileForm.state} 
                onChange={(e) => setProfileForm({ ...profileForm, state: e.target.value })} 
                maxLength={2}
                disabled={loadingCep}
                required 
              />
            </div>

            <div className="profile-form-actions">
              <button type="submit" disabled={loadingCep}>
                {loadingCep ? 'Buscando...' : 'Salvar alterações'}
              </button>
              <button type="button" onClick={() => setIsEditingProfile(false)} className="danger-button" disabled={loadingCep}>
                Cancelar
              </button>
            </div>
          </form>
        )}
      </Panel>

      <Panel title="Cartões salvos">
        <div className="card-save-workspace">
          <div className={`saved-card-preview ${cardSaved ? 'saved' : ''}`}>
            <span>ACC Pay</span>
            <strong>{cardPreviewNumber}</strong>
            <div>
              <small>{cardPreviewHolder}</small>
              <small>{cardPreviewExpiry}</small>
            </div>
          </div>

          <form onSubmit={registerCard} className="stack-form compact wide card-save-form">
            <input 
              placeholder="Número do cartão" 
              value={card.cardNumber} 
              onChange={(event) => setCard({ ...card, cardNumber: digitsOnly(event.target.value).slice(0, 16) })} 
              inputMode="numeric"
              maxLength={16}
              required 
            />
            <div className="three-col">
              <input 
                placeholder="CVV" 
                value={card.cvv} 
                onChange={(event) => setCard({ ...card, cvv: digitsOnly(event.target.value).slice(0, 4) })} 
                inputMode="numeric"
                maxLength={4}
                required 
              />
              <input 
                placeholder="Mês" 
                value={card.expirationMonth} 
                onChange={(event) => setCard({ ...card, expirationMonth: digitsOnly(event.target.value).slice(0, 2) })} 
                inputMode="numeric"
                maxLength={2}
                required 
              />
              <input 
                placeholder="Ano" 
                value={card.expirationYear} 
                onChange={(event) => setCard({ ...card, expirationYear: digitsOnly(event.target.value).slice(0, 4) })} 
                inputMode="numeric"
                maxLength={4}
                required 
              />
            </div>
            <input 
              placeholder="Nome impresso" 
              value={card.holderName} 
              onChange={(event) => setCard({ ...card, holderName: event.target.value.toUpperCase() })} 
            />
            {cardSaved ? <small className="card-save-feedback">Cartão salvo com sucesso.</small> : null}
            <button disabled={savingCard}>{savingCard ? 'Salvando...' : cardSaved ? 'Cartão salvo' : 'Cadastrar cartão'}</button>
          </form>
        </div>
        <List 
          items={cards} 
          render={(saved: SavedCard) => (
            <span className="saved-card-line">
              <b>{saved.holderName || 'Cartao salvo'}</b>
              <small>Final {saved.last4Digits}</small>
            </span>
          )} 
          action={(saved: SavedCard) => <button onClick={() => removeCard(saved.id)}>Excluir</button>} 
        />
      </Panel>

      <Panel title="Meus pedidos">
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '16px',
            alignItems: 'flex-start'
          }}
        >
          <p
            style={{
              margin: 0,
              color: '#c9b8dc',
              fontSize: '14px',
              lineHeight: '1.5'
            }}
          >
            Consulte o histórico de todas as suas compras,
            status de pagamento e detalhes de envio.
          </p>

          <button type="button" onClick={() => navigate('/pedidos')}>
            Consultar
          </button>
        </div>
      </Panel>
    </div>
  )
}
