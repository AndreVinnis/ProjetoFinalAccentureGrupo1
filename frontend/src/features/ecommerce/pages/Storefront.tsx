import { useCallback, useEffect, useState } from 'react'
import { Panel } from '../../../components/ui/Panel'
import { placeholderImageForCategory } from '../../../categoryPlaceholder'
import { settled } from '../../../utils/async'
import { money } from '../../../utils/format'
import type { Product } from '../types/product'
import type { Category } from '../types/category'
import type { ApiClient } from '../../../services/api'

export function Storefront({ api }: { api: ApiClient }) {
  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [filters, setFilters] = useState({ categoryName: '', maxPrice: '' })
  const [addingProductId, setAddingProductId] = useState<number | null>(null)
  const [addedProductId, setAddedProductId] = useState<number | null>(null)

    const refresh = useCallback(async () => {
        const query = new URLSearchParams()

        if (filters.categoryName) {query.set('categoryName', filters.categoryName)}
        if (filters.maxPrice) {query.set('maxPrice', filters.maxPrice)}

        const [productsResult, categoriesResult] =await Promise.allSettled([
            api.get<{ content: Product[] }>(`/ecommerce/products?${query.toString()}`),
            api.get<Category[]>('/ecommerce/categories')
        ])

        return {
            products: settled(productsResult)?.content ?? [],
            categories: settled(categoriesResult, []) ?? []
        }
    }, [api, filters.categoryName, filters.maxPrice])

    useEffect(() => {async function load() {
        const data = await refresh()

        setProducts(data.products)
        setCategories(data.categories)
    }

    void load()}, [refresh])

    async function addToCart(productId: number) {
      setAddingProductId(productId)

      try {
        await api.post('/ecommerce/cart/me/items', {
          productId,
          quantity: 1
        })

        const data = await refresh()

        setProducts(data.products)
        setCategories(data.categories)
        setAddedProductId(productId)
        window.dispatchEvent(new CustomEvent('acc-cart-added'))

        window.setTimeout(() => {
          setAddedProductId((current) => (current === productId ? null : current))
        }, 1400)
      } finally {
        setAddingProductId(null)
      }
    }

  return (
    <div className="dashboard-grid ecommerce">
      <Panel title="Vitrine">
        <form className="inline-form" onSubmit={(event) => { event.preventDefault(); refresh() }}>
          <select value={filters.categoryName} onChange={(event) => setFilters({ ...filters, categoryName: event.target.value })}>
            <option value="">Todas categorias</option>
            {categories.map((category) => <option key={category.id} value={category.name}>{category.name}</option>)}
          </select>
          <input placeholder="Preco maximo" value={filters.maxPrice} onChange={(event) => setFilters({ ...filters, maxPrice: event.target.value })} type="number" />
        </form>
        <div className="product-grid">
          {products.map((product) => (
            <article className="product-card product-card--visual" key={product.id}>
              <div className="product-card-cover">
                <img src={placeholderImageForCategory(product.categoryName)} alt="" loading="lazy" decoding="async" />
                <span className="product-card-badge">{product.categoryName}</span>
              </div>
              <h3>{product.name}</h3>
              <p>{product.description}</p>
              <strong>{money(product.price)}</strong>
              <button
                className={`product-add-button ${addedProductId === product.id ? 'added' : ''}`}
                onClick={() => addToCart(product.id)}
                disabled={addingProductId === product.id}
              >
                <span className="product-add-icon" aria-hidden="true" />
                {addingProductId === product.id ? 'Adicionando...' : addedProductId === product.id ? 'Na sacola' : 'Adicionar'}
              </button>
            </article>
          ))}
        </div>
      </Panel>
    </div>
  )
}
