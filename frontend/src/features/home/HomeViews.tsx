/* eslint-disable @typescript-eslint/ban-ts-comment */
/* eslint-disable react-hooks/set-state-in-effect */
// @ts-nocheck
import { useCallback, useEffect, useState } from 'react'
import { placeholderImageForCategory } from '../../categoryPlaceholder'
import { settled } from '../../utils/async'
import { money } from '../../utils/format'

export function CustomerHome({ api, setActiveView, setToast }) {
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

  useEffect(() => { refreshFeed() }, [refreshFeed])

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
            <button type="button" onClick={() => setActiveView('customerEcommerce')}>Abrir loja completa</button>
            <button type="button" className="ghost-button light-ghost" onClick={() => setActiveView('customerBank')}>Ir ao banco</button>
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
                  <small>{item.quantity}x {money(item.unitPrice)}</small>
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
          <button type="button" className="primary-button block" onClick={() => setActiveView('customerEcommerce')}>Ver carrinho e checkout</button>
        </aside>
      </div>

      <div className="feed-toolbar">
        <p className="eyebrow">Categorias</p>
        <div className="feed-chips">
          <button type="button" className={categoryFilter === '' ? 'active' : ''} onClick={() => setCategoryFilter('')}>Todas</button>
          {categories.map((c) => (
            <button type="button" key={c.id} className={categoryFilter === c.name ? 'active' : ''} onClick={() => setCategoryFilter(c.name)}>{c.name}</button>
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
                <img src={placeholderImageForCategory(product.categoryName)} alt="" loading="lazy" decoding="async" />
                <span className="feed-card-chip">{product.categoryName}</span>
              </div>
              <div className="feed-card-body">
                <h3>{product.name}</h3>
                <p>{product.description}</p>
                <div className="feed-card-meta">
                  <strong>{money(product.price)}</strong>
                  <small>Estoque {product.availableStock}</small>
                </div>
                <button type="button" onClick={() => addFromFeed(product.id)}>Adicionar ao carrinho</button>
              </div>
            </article>
          ))}
        </div>
      )}

      {!feedLoading && !products.length ? <p className="empty-state">Nenhum produto neste filtro.</p> : null}

      <div className="journey-grid feed-journey">
        <article><strong>1. Comprar</strong><span>Monte o carrinho pelo feed ou pela loja.</span></article>
        <article><strong>2. Pagar</strong><span>Pix ou cartao salvo na area da loja.</span></article>
        <article><strong>3. Acompanhar</strong><span>Pedidos e banco na mesma conta.</span></article>
      </div>
    </section>
  )
}

export function AdminHome({ roles, setActiveView }) {
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
        {roles.includes('ECOMMERCE_ADMIN') && <article><strong>Ecommerce</strong><span>Categorias, produtos, estoque e emails transacionais.</span></article>}
        {roles.includes('BANKING_ADMIN') && <article><strong>Banco</strong><span>Contas, bloqueios, depositos, transacoes e faturamento.</span></article>}
        <article><strong>Perfis separados</strong><span>CUSTOMER opera a experiencia de compra e banco; ADMIN opera gestao.</span></article>
      </div>
    </section>
  )
}
