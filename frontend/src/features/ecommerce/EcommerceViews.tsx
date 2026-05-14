/* eslint-disable @typescript-eslint/ban-ts-comment */
/* eslint-disable react-hooks/set-state-in-effect */
// @ts-nocheck
import { useCallback, useEffect, useState } from 'react'
import { List } from '../../components/ui/List'
import { Panel } from '../../components/ui/Panel'
import { Table, TablePanel } from '../../components/ui/Table'
import { placeholderImageForCategory } from '../../categoryPlaceholder'
import { settled } from '../../utils/async'
import { money } from '../../utils/format'

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

export function CustomerEcommerce({ api }) {
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

  useEffect(() => { refresh() }, [refresh])

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
    await api.post('/ecommerce/cards', { ...card, expirationMonth: Number(card.expirationMonth), expirationYear: Number(card.expirationYear) })
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
                <img src={placeholderImageForCategory(product.categoryName)} alt="" loading="lazy" decoding="async" />
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

export function AdminEcommerce({ api }) {
  const [categories, setCategories] = useState([])
  const [category, setCategory] = useState({ id: '', name: '', description: '' })
  const [product, setProduct] = useState({ id: '', name: '', description: '', price: '', initialStock: '', categoryName: '' })
  const [restock, setRestock] = useState({ id: '', quantity: '' })
  const [emails, setEmails] = useState([])
  const [emailFilter, setEmailFilter] = useState({ status: '', type: '', id: '' })

  const refresh = useCallback(async () => {
    const [categoryList, emailPage] = await Promise.allSettled([api.get('/ecommerce/categories'), api.get('/admin/notifications/emails')])
    setCategories(settled(categoryList, []))
    setEmails(settled(emailPage)?.content || [])
  }, [api])

  useEffect(() => { refresh() }, [refresh])

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
    const body = { name: product.name, description: product.description, price: Number(product.price), initialStock: Number(product.initialStock || 0), categoryName: product.categoryName }
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
            <option value="">Todos status</option><option value="PENDING">PENDING</option><option value="SENT">SENT</option><option value="FAILED">FAILED</option>
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
