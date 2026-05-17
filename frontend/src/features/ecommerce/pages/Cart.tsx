import '../../../styles/ecommerce/cart.css'
import { useCallback, useEffect, useState } from 'react'
import { Panel } from '../../../components/ui/Panel'
import { CartView } from '../components/CartView'
import { settled } from '../../../utils/async'
import type { ApiClient } from '../../../services/api'
import type { Cart } from '../types/cart' 
import type { Product } from '../types/product'
import type { SavedCard } from '../types/savedCard'

export function Cart({ api }: { api: ApiClient }) {
  const [cart, setCart] = useState<Cart | null>(null)
  const [cards, setCards] = useState<SavedCard[]>([])
  const [productCategories, setProductCategories] = useState<Record<number, string>>({})
  const [checkoutCard, setCheckoutCard] = useState({ savedCardId: '', cvv: '' })
  const [pixModal, setPixModal] = useState({ open: false, loading: false, code: '', copied: false })

  const refresh = useCallback(async () => {
    const [cartResult, cardsResult, productsResult] = await Promise.allSettled([
      api.get<Cart>('/ecommerce/cart/me'),
      api.get<SavedCard[]>('/ecommerce/cards'),
      api.get<{ content: Product[] }>('/ecommerce/products')
    ])
    const products = settled(productsResult)?.content ?? []

    return {
      cart: settled(cartResult),
      cards: settled(cardsResult, []) ?? [],
      productCategories: Object.fromEntries(products.map((product) => [product.id, product.categoryName]))
    }
  }, [api])

  useEffect(() => {
    async function load() {
      const data = await refresh()

      setCart(data.cart ?? null)
      setCards(data.cards)
      setProductCategories(data.productCategories)
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
    setProductCategories(data.productCategories)
  }

  async function removeCartItem(id: number) {
    await api.delete(`/ecommerce/cart/me/items/${id}`)

    const data = await refresh()

    setCart(data.cart ?? null)
    setCards(data.cards)
    setProductCategories(data.productCategories)
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
    setProductCategories(data.productCategories)
  }

  async function checkoutPix() {
    setPixModal({ open: true, loading: true, code: '', copied: false })

    try {
      const [code] = await Promise.all([
        api.post('/ecommerce/orders/checkout/pix'),
        new Promise((resolve) => window.setTimeout(resolve, 1000)),
      ])

      const pixCode = typeof code === 'string' ? code : code?.code || code?.pixCode || code?.hash || JSON.stringify(code)
      setPixModal({ open: true, loading: false, code: pixCode, copied: false })

      const data = await refresh()

      setCart(data.cart ?? null)
      setCards(data.cards)
      setProductCategories(data.productCategories)
    } catch {
      setPixModal({ open: true, loading: false, code: 'Nao foi possivel gerar o codigo Pix.', copied: false })
    }
  }

  async function copyPixCode() {
    if (!pixModal.code) return

    if (navigator.clipboard) {
      await navigator.clipboard.writeText(pixModal.code)
    } else {
      const textArea = document.createElement('textarea')
      textArea.value = pixModal.code
      document.body.appendChild(textArea)
      textArea.select()
      document.execCommand('copy')
      textArea.remove()
    }

    setPixModal((current) => ({ ...current, copied: true }))
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
    setProductCategories(data.productCategories)
  }

  return (
    <div className="dashboard-grid ecommerce account-workspace">
      <Panel title="Carrinho">
        <CartView 
          cart={cart} 
          productCategories={productCategories}
          onUpdate={updateCart} 
          onRemove={removeCartItem} 
        />
        <div className="button-row">
        {cart?.status === 'RESERVED' ? (
          <button onClick={() => handleCartAction('open')}>Reabrir carrinho</button>
          ) : (
          <button onClick={() => handleCartAction('close')}>Fechar carrinho</button>
          )}
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

      {pixModal.open ? (
        <div className="cart-pix-modal-backdrop" role="presentation" onMouseDown={() => setPixModal({ open: false, loading: false, code: '', copied: false })}>
          <section className="cart-pix-modal" role="dialog" aria-modal="true" aria-label="Codigo Pix" onMouseDown={(event) => event.stopPropagation()}>
            <button className="modal-close-button" type="button" onClick={() => setPixModal({ open: false, loading: false, code: '', copied: false })} aria-label="Fechar modal">x</button>
            {pixModal.loading ? (
              <div className="cart-pix-loading" role="status">
                <span className="cart-pix-loader" aria-hidden="true" />
                <strong>Gerando Pix</strong>
                <small>Preparando o codigo de pagamento da sua sacola.</small>
              </div>
            ) : (
              <div className="cart-pix-ready">
                <span>Checkout Pix</span>
                <strong>Codigo Pix gerado</strong>
                <code>{pixModal.code}</code>
                <button type="button" onClick={copyPixCode}>{pixModal.copied ? 'Copiado' : 'Copiar codigo'}</button>
              </div>
            )}
          </section>
        </div>
      ) : null}
    </div>
  )
}
