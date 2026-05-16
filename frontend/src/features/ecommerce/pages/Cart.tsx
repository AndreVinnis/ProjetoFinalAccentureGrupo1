import '../../../styles/ecommerce/cart.css'
import { useCallback, useEffect, useState } from 'react'
import { Panel } from '../../../components/ui/Panel'
import { CartView } from '../components/CartView'
import { settled } from '../../../utils/async'
import type { ApiClient } from '../../../services/api'
import type { Cart } from '../types/cart' 
import type { SavedCard } from '../types/savedCard'

export function Cart({ api }: { api: ApiClient }) {
  const [cart, setCart] = useState<Cart | null>(null)
  const [cards, setCards] = useState<SavedCard[]>([])
  const [checkoutCard, setCheckoutCard] = useState({ savedCardId: '', cvv: '' })

  const refresh = useCallback(async () => {
    const [cartResult, cardsResult] = await Promise.allSettled([
      api.get<Cart>('/ecommerce/cart/me'),
      api.get<SavedCard[]>('/ecommerce/cards')
    ])

    return {
      cart: settled(cartResult),
      cards: settled(cardsResult, []) ?? []
    }
  }, [api])

  useEffect(() => {
    async function load() {
      const data = await refresh()

      setCart(data.cart ?? null)
      setCards(data.cards)
    }

    void load()
  }, [refresh])

  async function updateCart(productId: number, quantity: number) {
    await api.put(`/ecommerce/cart/me/items/${productId}`, { 
      quantity: quantity 
    })

    const data = await refresh()

    setCart(data.cart ?? null)
    setCards(data.cards)
  }

  async function removeCartItem(id: number) {
    await api.delete(`/ecommerce/cart/me/items/${id}`)

    const data = await refresh()

    setCart(data.cart ?? null)
    setCards(data.cards)
  }

  async function handleCartAction(action: 'close' | 'open' | 'clear') {
    if (action === 'clear') {
      await api.delete('/ecommerce/cart/me')
    } else {
      await api.patch(`/ecommerce/cart/${action}/me`)
    }

    const data = await refresh()

    setCart(data.cart ?? null)
    setCards(data.cards)
  }

  async function checkoutPix() {
    const code = await api.post('/ecommerce/orders/checkout/pix')
    
    alert(`Codigo Pix: ${code}`)
    
    const data = await refresh()

    setCart(data.cart ?? null)
    setCards(data.cards)
  }

  async function payWithCard(event: React.FormEvent) {
    event.preventDefault()
    
    await api.post('/ecommerce/orders/checkout/card', { 
      savedCardId: Number(checkoutCard.savedCardId), 
      cvv: checkoutCard.cvv 
    })
    
    setCheckoutCard({ savedCardId: '', cvv: '' })
    
    const data = await refresh()

    setCart(data.cart ?? null)
    setCards(data.cards)
  }

  return (
    <div className="dashboard-grid ecommerce account-workspace">
      <Panel title="Carrinho">
        <CartView 
          cart={cart} 
          onUpdate={updateCart} 
          onRemove={removeCartItem} 
        />
        <div className="button-row">
          {/* Botão dinâmico que alterna entre Fechar e Reabrir */}
{(cart as any)?.status === 'RESERVED' ? (
  <button onClick={() => handleCartAction('open')}>Reabrir carrinho</button>
) : (
  <button onClick={() => handleCartAction('close')}>Fechar carrinho</button>
)}
<button onClick={() => handleCartAction('clear')}>Limpar</button>
<button onClick={checkoutPix}>Checkout Pix</button>
          <button onClick={() => handleCartAction('clear')}>Limpar</button>
          <button onClick={checkoutPix}>Checkout Pix</button>
        </div>
        <form onSubmit={payWithCard} className="inline-form">
          <select 
            value={checkoutCard.savedCardId} 
            onChange={(event) => setCheckoutCard({ ...checkoutCard, savedCardId: event.target.value })} 
            required
          >
            <option value="">Cartão salvo</option>
            {cards.map((saved) => (
              <option key={saved.id} value={saved.id}>
                {saved.holderName} final {saved.last4Digits}
              </option>
            ))}
          </select>
          <input 
            placeholder="CVV" 
            value={checkoutCard.cvv} 
            onChange={(event) => setCheckoutCard({ ...checkoutCard, cvv: event.target.value })} 
            required 
          />
          <button>Pagar com cartão</button>
        </form>
      </Panel>
    </div>
  )
}