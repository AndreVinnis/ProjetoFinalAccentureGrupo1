import '../../../styles/ecommerce/cart.css'
import { useCallback, useEffect, useState } from 'react'
import { Panel } from '../../../components/ui/Panel'
import { CartView } from '../components/CartView'
import { settled } from '../../../utils/async'
import { money } from '../../../utils/format'
import type { ApiClient } from '../../../services/api'
import type { Cart } from '../types/cart' 
import type { Product } from '../types/product'
import type { SavedCard } from '../types/savedCard'

interface InstallmentOption {
  installments: number
  installmentAmount: number
  totalAmount: number
  label?: string
}

export function Cart({ api }: { api: ApiClient }) {
  const [cart, setCart] = useState<Cart | null>(null)
  const [cards, setCards] = useState<SavedCard[]>([])
  const [installmentOptions, setInstallmentOptions] = useState<InstallmentOption[]>([])
  const [productCategories, setProductCategories] = useState<Record<number, string>>({})
  const [checkoutCard, setCheckoutCard] = useState({ savedCardId: '', cvv: '', installments: '1' })
  const [pixModal, setPixModal] = useState({ open: false, loading: false, code: '', copied: false })
  const [cardSuccessOpen, setCardSuccessOpen] = useState(false)

  const refresh = useCallback(async () => {
    const [cartResult, cardsResult, installmentsResult, productsResult] = await Promise.allSettled([
      api.get<Cart>('/ecommerce/cart/me'),
      api.get<SavedCard[]>('/ecommerce/cards'),
      api.get<InstallmentOption[]>('/ecommerce/orders/checkout/card/installments', { silent: true }),
      api.get<{ content: Product[] }>('/ecommerce/products')
    ])
    const products = settled(productsResult)?.content ?? []

    return {
      cart: settled(cartResult),
      cards: settled(cardsResult, []) ?? [],
      installmentOptions: settled(installmentsResult, []) ?? [],
      productCategories: Object.fromEntries(products.map((product) => [product.id, product.categoryName]))
    }
  }, [api])

  useEffect(() => {
    async function load() {
      const data = await refresh()

      setCart(data.cart ?? null)
      setCards(data.cards)
      setInstallmentOptions(data.installmentOptions)
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
    setInstallmentOptions(data.installmentOptions)
    setProductCategories(data.productCategories)
  }

  async function removeCartItem(id: number) {
    await api.delete(`/ecommerce/cart/me/items/${id}`)

    const data = await refresh()

    setCart(data.cart ?? null)
    setCards(data.cards)
    setInstallmentOptions(data.installmentOptions)
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
    setInstallmentOptions(data.installmentOptions)
    setProductCategories(data.productCategories)
  }

  async function checkoutPix() {
    setPixModal({ open: true, loading: true, code: '', copied: false })

    try {
      const [code] = await Promise.all([
        api.post('/ecommerce/orders/checkout/pix'),
        new Promise((resolve) => window.setTimeout(resolve, 1000)),
      ])

      const pixPayload = code as string | Partial<Record<'code' | 'pixCode' | 'hash', string>>
      const pixCode = typeof pixPayload === 'string'
        ? pixPayload
        : pixPayload.code || pixPayload.pixCode || pixPayload.hash || JSON.stringify(code)
      setPixModal({ open: true, loading: false, code: pixCode, copied: false })

      const data = await refresh()

      setCart(data.cart ?? null)
      setCards(data.cards)
      setInstallmentOptions(data.installmentOptions)
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

  function formatInstallmentOption(option: InstallmentOption) {
    if (option.installments === 1) {
      return `A vista - ${money(option.totalAmount)}`
    }

    return `${option.installments}x de ${money(option.installmentAmount)} sem juros`
  }

  async function payWithCard(event: React.FormEvent) {
    event.preventDefault()
    
    await api.post('/ecommerce/orders/checkout/card', { 
      savedCardId: Number(checkoutCard.savedCardId), 
      cvv: checkoutCard.cvv,
      installments: Number(checkoutCard.installments)
    })
    
    setCheckoutCard({ savedCardId: '', cvv: '', installments: '1' })
    
    const data = await refresh()

    setCart(data.cart ?? null)
    setCards(data.cards)
    setInstallmentOptions(data.installmentOptions)
    setProductCategories(data.productCategories)
    setCardSuccessOpen(true)
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
          <select
            value={checkoutCard.installments}
            onChange={(event) => setCheckoutCard({ ...checkoutCard, installments: event.target.value })}
            required
          >
            {installmentOptions.length ? installmentOptions.map((option) => (
              <option key={option.installments} value={option.installments}>
                {formatInstallmentOption(option)}
              </option>
            )) : (
              <option value="1">A vista - {money(cart?.subtotal ?? 0)}</option>
            )}
          </select>
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

      {cardSuccessOpen ? (
        <div className="cart-pix-modal-backdrop" role="presentation" onMouseDown={() => setCardSuccessOpen(false)}>
          <section className="cart-card-success-modal" role="dialog" aria-modal="true" aria-label="Pagamento realizado" onMouseDown={(event) => event.stopPropagation()}>
            <button className="modal-close-button" type="button" onClick={() => setCardSuccessOpen(false)} aria-label="Fechar modal">x</button>
            <div className="cart-card-success-content">
              <span className="cart-card-success-check" aria-hidden="true">✓</span>
              <span>Cartao aprovado</span>
              <strong>Pagamento realizado com sucesso</strong>
              <small>Sua compra foi confirmada e o pedido ja aparece em Meus Pedidos.</small>
              <button type="button" onClick={() => setCardSuccessOpen(false)}>Fechar</button>
            </div>
          </section>
        </div>
      ) : null}
    </div>
  )
}
