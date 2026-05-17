import { money } from '../../../utils/format'
import { placeholderImageForCategory } from '../../../categoryPlaceholder'
import type { Cart } from '../types/cart'

interface CartViewProps {
  cart: Cart | null
  productCategories?: Record<number, string>
  onUpdate: (productId: number, quantity: number) => void
  onRemove: (productId: number) => void
}

export function CartView({ cart, productCategories = {}, onUpdate, onRemove }: CartViewProps) {
  if (!cart?.items?.length) {
    return <p className="empty-state">Carrinho vazio.</p>
  }

  return (
    <div className="cart-list">
      {cart.items.map((item) => (
        <div className="cart-row" key={item.productId}>
          <div className="cart-row-cover">
            <img src={placeholderImageForCategory(productCategories[item.productId] || item.categoryName || item.productName)} alt="" loading="lazy" decoding="async" />
          </div>

          <div>
            <strong>{item.productName}</strong>
            <small>{money(item.unitPrice)} cada</small>
          </div>

          <input
            value={item.quantity}
            min={0}
            type="number"
            onChange={(event) =>
              onUpdate(
                item.productId,
                Number(event.target.value)
              )
            }
          />

          <strong>{money(item.lineTotal)}</strong>

          <button onClick={() => onRemove(item.productId)}>
            Remover
          </button>
        </div>
      ))}

      <div className="cart-total">
        Subtotal {money(cart.subtotal)}
      </div>
    </div>
  )
}
