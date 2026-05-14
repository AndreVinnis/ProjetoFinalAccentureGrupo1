import { useCallback, useEffect, useState } from 'react'
import { Panel } from '../../../components/ui/Panel'
import { List } from '../../../components/ui/List'
import { TablePanel } from '../../../components/ui/Table'
import { settled } from '../../../utils/async'  
import type { ApiClient } from '../../../services/api'
import type { Order } from '../types/order'
import type { SavedCard } from '../types/savedCard'
import type { Customer } from '../types/customer'

export function Profile({ api }: { api: ApiClient }) {
  const [orders, setOrders] = useState<Order[]>([])
  const [cards, setCards] = useState<SavedCard[]>([])
  const [customer, setCustomer] = useState<Customer | null>(null)
  const [profile, setProfile] = useState({ shippingAddress: '', phone: '' })
  const [card, setCard] = useState({ cardNumber: '', cvv: '', expirationMonth: '', expirationYear: '', holderName: '' })
  const [selectedOrder, setSelectedOrder] = useState<number | null>(null)

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
      
      setOrders(data.orders)
      setCards(data.cards)
      setCustomer(data.customer)
      if (data.profile) setProfile(data.profile)
    }
    
    void load()
  }, [refresh])

  async function registerCard(event: React.FormEvent) {
    event.preventDefault()
    
    await api.post('/ecommerce/cards', { 
      ...card, 
      expirationMonth: Number(card.expirationMonth), 
      expirationYear: Number(card.expirationYear) 
    })
    
    setCard({ cardNumber: '', cvv: '', expirationMonth: '', expirationYear: '', holderName: '' })
    
    const data = await refresh()
    
    setOrders(data.orders)
    setCards(data.cards)
    setCustomer(data.customer)
    if (data.profile) setProfile(data.profile)
  }

  async function removeCard(id: number) {
    await api.delete(`/ecommerce/cards/${id}`)
    
    const data = await refresh()
    
    setOrders(data.orders)
    setCards(data.cards)
    setCustomer(data.customer)
    if (data.profile) setProfile(data.profile)
  }

  async function updateProfile(event: React.FormEvent) {
    event.preventDefault()
    
    await api.put('/customers/me', profile)
    
    const data = await refresh()
    
    setOrders(data.orders)
    setCards(data.cards)
    setCustomer(data.customer)
    if (data.profile) setProfile(data.profile)
  }

  async function viewOrder(orderId: number) {
    await api.get(`/ecommerce/orders/${orderId}`)
    setSelectedOrder(orderId)
  }

  async function cancelOrder(orderId: number) {
    await api.post(`/ecommerce/orders/${orderId}/cancel`)
    
    const data = await refresh()
    
    setOrders(data.orders)
    setCards(data.cards)
    setCustomer(data.customer)
    if (data.profile) setProfile(data.profile)
  }

  return (
    <div className="dashboard-grid ecommerce account-workspace">
      <Panel title="Informações do cliente">
        <form onSubmit={updateProfile} className="stack-form compact wide">
          <textarea 
            placeholder="Endereço de entrega" 
            value={profile.shippingAddress} 
            onChange={(event) => setProfile({ ...profile, shippingAddress: event.target.value })} 
            required 
          />
          <input 
            placeholder="Telefone" 
            value={profile.phone} 
            onChange={(event) => setProfile({ ...profile, phone: event.target.value })} 
            required 
          />
          <button>Atualizar perfil</button>
        </form>
        <small>Tier atual: {customer?.tier || 'indisponível'} - compras: {customer?.quantityPurchases ?? 0}</small>
      </Panel>

      <Panel title="Cartões salvos">
        <form onSubmit={registerCard} className="stack-form compact wide">
          <input 
            placeholder="Número do cartão" 
            value={card.cardNumber} 
            onChange={(event) => setCard({ ...card, cardNumber: event.target.value })} 
            required 
          />
          <div className="three-col">
            <input 
              placeholder="CVV" 
              value={card.cvv} 
              onChange={(event) => setCard({ ...card, cvv: event.target.value })} 
              required 
            />
            <input 
              placeholder="Mês" 
              value={card.expirationMonth} 
              onChange={(event) => setCard({ ...card, expirationMonth: event.target.value })} 
              required 
            />
            <input 
              placeholder="Ano" 
              value={card.expirationYear} 
              onChange={(event) => setCard({ ...card, expirationYear: event.target.value })} 
              required 
            />
          </div>
          <input 
            placeholder="Nome impresso" 
            value={card.holderName} 
            onChange={(event) => setCard({ ...card, holderName: event.target.value })} 
          />
          <button>Cadastrar cartão</button>
        </form>
        <List 
          items={cards} 
          render={(saved: SavedCard) => `${saved.holderName} - final ${saved.last4Digits}`} 
          action={(saved: SavedCard) => <button onClick={() => removeCard(saved.id)}>Excluir</button>} 
        />
      </Panel>

      <TablePanel<Order>
            title="Pedidos"
            rows={orders}
            columns={[
                'orderId',
                'status',
                'paymentMethod',
                'subtotal',
                'discountTotal',
                'totalAmount',
                'createdAt'
            ]}
            action={(order) => (
                <button onClick={() => viewOrder(order.orderId)}>
                Ver
                </button>
            )}
        />
      {selectedOrder && (
        <button className="danger-button" onClick={() => cancelOrder(selectedOrder)}>
          Cancelar pedido {selectedOrder}
        </button>
      )}
    </div>
  )
}