import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { Panel } from '../../../../components/ui/Panel'
import { TablePanel } from '../../../../components/ui/Table'
import { placeholderImageForCategory } from '../../../../categoryPlaceholder'
import { settled } from '../../../../utils/async'
import { money } from '../../../../utils/format'
import type { ApiClient } from '../../../../services/api'
import type { Category } from '../../types/category'
import type { Product } from '../../types/product'

interface EmailLog {
  id: number
  toEmail: string
  subject: string
  type: string
  status: string
  createdAt: string
}

interface Page<T> {
  content: T[]
}

interface ProductForm {
  name: string
  description: string
  price: string
  initialStock: string
  categoryName: string
}

interface ProductEditForm extends ProductForm {
  id: string
}

interface CategoryForm {
  id: string
  name: string
  description: string
}

const emptyProductForm: ProductForm = {
  name: '',
  description: '',
  price: '',
  initialStock: '',
  categoryName: ''
}

const emptyProductEditForm: ProductEditForm = {
  id: '',
  ...emptyProductForm
}

const emptyCategoryForm: CategoryForm = {
  id: '',
  name: '',
  description: ''
}

export function AdminEcommerce({ api }: { api: ApiClient }) {
  const editProductRef = useRef<HTMLElement | null>(null)
  const stockPanelRef = useRef<HTMLElement | null>(null)
  const [emails, setEmails] = useState<EmailLog[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [productForm, setProductForm] = useState<ProductForm>(emptyProductForm)
  const [productEditForm, setProductEditForm] = useState<ProductEditForm>(emptyProductEditForm)
  const [categoryForm, setCategoryForm] = useState<CategoryForm>(emptyCategoryForm)
  const [stockAction, setStockAction] = useState({ productId: '', quantity: '' })
  const [feedback, setFeedback] = useState('')
  const [productsLoadError, setProductsLoadError] = useState('')
  const [busyAction, setBusyAction] = useState('')
  const totalAvailableStock = useMemo(
    () => products.reduce((total, product) => total + getAvailableStock(product), 0),
    [products]
  )

  const lowStockProducts = useMemo(
    () => products.filter((product) => getAvailableStock(product) <= 5),
    [products]
  )

  const refresh = useCallback(async () => {
    const [emailPage, adminProductPage, publicProductPage, categoryList] = await Promise.allSettled([
      api.get<Page<EmailLog>>('/admin/notifications/emails', { silent: true }),
      api.get<Page<Product>>('/ecommerce/admin/products?size=100', { silent: true }),
      api.get<Page<Product>>('/ecommerce/products?size=100', { silent: true }),
      api.get<Category[]>('/ecommerce/categories', { silent: true })
    ])

    const productData = settled(adminProductPage) ?? settled(publicProductPage)

    setEmails(settled(emailPage)?.content ?? [])
    setProducts(productData?.content ?? [])
    setCategories(settled(categoryList, []) ?? [])
    setProductsLoadError(productData ? '' : 'Nao foi possivel consultar produtos. Reinicie o backend para carregar o novo endpoint admin.')
  }, [api])

  useEffect(() => {
    const loadTimer = window.setTimeout(() => {
      void refresh()
    }, 0)

    return () => window.clearTimeout(loadTimer)
  }, [refresh])

  async function submitProduct(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await runAction('create-product', async () => {
      await api.post('/ecommerce/admin/products', toProductPayload(productForm))
      setProductForm(emptyProductForm)
      setFeedback('Produto cadastrado na vitrine.')
    })
  }

  async function submitProductEdit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await runAction('edit-product', async () => {
      await api.put(
        `/ecommerce/admin/products/${productEditForm.id}`,
        toProductPayload(productEditForm)
      )
      setProductEditForm(emptyProductEditForm)
      setFeedback('Produto atualizado com sucesso.')
    })
  }

  async function submitCategory(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await runAction('save-category', async () => {
      const payload = {
        name: categoryForm.name,
        description: categoryForm.description
      }

      if (categoryForm.id) {
        await api.put(`/ecommerce/admin/categories/${categoryForm.id}`, payload)
        setFeedback('Categoria atualizada.')
      } else {
        await api.post('/ecommerce/admin/categories', payload)
        setFeedback('Categoria criada.')
      }

      setCategoryForm(emptyCategoryForm)
    })
  }

  async function restockProduct(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await runAction('restock', async () => {
      const quantity = Number(stockAction.quantity)

      await api.post(
        `/ecommerce/admin/products/${stockAction.productId}/restock?quantity=${quantity}`
      )
      setStockAction({ productId: '', quantity: '' })
      setFeedback('Estoque atualizado.')
    })
  }

  async function deactivateProduct(productId: number | string) {
    await runAction(`deactivate-${productId}`, async () => {
      await api.post(`/ecommerce/admin/products/${productId}/deactivate`)
      setFeedback('Produto desativado.')
    })
  }

  async function activateProduct(productId: number | string) {
    await runAction(`activate-${productId}`, async () => {
      await api.post(`/ecommerce/admin/products/${productId}/activate`)
      setFeedback('Produto ativado.')
    })
  }

  async function toggleProductStatus(product: Product) {
    if (product.active === false) {
      await activateProduct(product.id)
      return
    }

    await deactivateProduct(product.id)
  }

  async function deleteCategory(categoryId: number) {
    await runAction(`delete-category-${categoryId}`, async () => {
      await api.delete(`/ecommerce/admin/categories/${categoryId}`)
      setFeedback('Categoria removida.')
    })
  }

  async function runAction(action: string, callback: () => Promise<void>) {
    setBusyAction(action)

    try {
      await callback()
      await refresh()
    } finally {
      setBusyAction('')
    }
  }

  function fillProductEdit(product: Product) {
    setProductEditForm({
      id: String(product.id),
      name: product.name,
      description: product.description,
      price: String(product.price),
      initialStock: String(getAvailableStock(product)),
      categoryName: product.categoryName
    })
  }

  function goToProductEdit(product: Product) {
    fillProductEdit(product)
    scrollToSection(editProductRef.current)
  }

  function goToRestock(product: Product) {
    setStockAction({ productId: String(product.id), quantity: '' })
    scrollToSection(stockPanelRef.current)
  }

  function fillCategoryEdit(category: Category) {
    setCategoryForm({
      id: String(category.id),
      name: category.name,
      description: category.description
    })
  }

  return (
    <div className="dashboard-grid admin ecommerce ecommerce-admin-workspace">
      <section className="admin-store-hero">
        <div>
          <span>Central Ecommerce</span>
          <h2>Controle da vitrine, estoque e comunicados da loja.</h2>
          <p>Gerencie produtos e categorias com ações rápidas, mantendo o mesmo clima visual da experiência do cliente.</p>
        </div>

        <div className="admin-store-metrics" aria-label="Resumo da loja">
          <article>
            <i className="admin-icon admin-icon-bag" aria-hidden="true" />
            <strong>{products.length}</strong>
            <small>produtos visiveis</small>
          </article>
          <article>
            <i className="admin-icon admin-icon-tag" aria-hidden="true" />
            <strong>{categories.length}</strong>
            <small>categorias</small>
          </article>
          <article>
            <i className="admin-icon admin-icon-stock" aria-hidden="true" />
            <strong>{totalAvailableStock}</strong>
            <small>itens disponiveis</small>
          </article>
        </div>
      </section>

      {feedback ? (
        <p className="toast-banner admin-feedback">{feedback}</p>
      ) : null}

      <Panel title="Produtos e estoques">
        <div className="admin-inventory-list">
          <div className="admin-inventory-head">
            <span>Produto</span>
            <span>Preco</span>
            <span>Estoque</span>
            <span>Status</span>
            <span>Acoes</span>
          </div>

          {products.map((product) => {
            const availableStock = getAvailableStock(product)
            const totalStock = getTotalStock(product)
            const reservedStock = getReservedStock(product)
            const stockPercent = totalStock ? Math.min(100, (availableStock / totalStock) * 100) : 0

            return (
              <article className={`admin-inventory-row ${availableStock <= 5 ? 'low-stock' : ''}`} key={product.id}>
                <div className="admin-inventory-product">
                  <img src={placeholderImageForCategory(product.categoryName)} alt="" loading="lazy" decoding="async" />
                  <div>
                    <strong>{product.name}</strong>
                    <small>#{product.id} · {product.categoryName}</small>
                  </div>
                </div>
                <strong>{money(product.price)}</strong>
                <div className="admin-stock-cell">
                  <div>
                    <strong>{availableStock}</strong>
                    <small>disp. de {totalStock}</small>
                  </div>
                  <div className="admin-stock-track" aria-hidden="true">
                    <i style={{ width: `${stockPercent}%` }} />
                  </div>
                  <small>{reservedStock} reservado{reservedStock === 1 ? '' : 's'}</small>
                </div>
                <span className={`admin-status-pill ${product.active === false ? 'inactive' : 'active'}`}>
                  {product.active === false ? 'Inativo' : availableStock <= 5 ? 'Baixo estoque' : 'Ativo'}
                </span>
                <div className="button-row tight admin-inventory-actions">
                  <button type="button" onClick={() => goToProductEdit(product)}>Editar</button>
                  <button type="button" onClick={() => goToRestock(product)}>Repor</button>
                  <button
                    type="button"
                    className={product.active === false ? '' : 'danger-button'}
                    onClick={() => toggleProductStatus(product)}
                    disabled={busyAction === `deactivate-${product.id}` || busyAction === `activate-${product.id}`}
                  >
                    {product.active === false ? 'Ativar' : 'Desativar'}
                  </button>
                </div>
              </article>
            )
          })}

          {!products.length ? (
            <p className="empty-state">{productsLoadError || 'Nenhum produto cadastrado no ecommerce.'}</p>
          ) : null}
        </div>
      </Panel>

      <Panel title="Novo produto">
        <form className="admin-commerce-form" onSubmit={submitProduct}>
          <label>
            Nome
            <input
              value={productForm.name}
              onChange={(event) => setProductForm({ ...productForm, name: event.target.value })}
              placeholder="Ex: Headset Gamer"
              required
            />
          </label>
          <label>
            Categoria
            <select
              value={productForm.categoryName}
              onChange={(event) => setProductForm({ ...productForm, categoryName: event.target.value })}
              required
            >
              <option value="">Selecione</option>
              {categories.map((category) => (
                <option key={category.id} value={category.name}>{category.name}</option>
              ))}
            </select>
          </label>
          <label>
            Preco
            <input
              value={productForm.price}
              onChange={(event) => setProductForm({ ...productForm, price: event.target.value })}
              type="number"
              min="0"
              step="0.01"
              placeholder="0,00"
              required
            />
          </label>
          <label>
            Estoque inicial
            <input
              value={productForm.initialStock}
              onChange={(event) => setProductForm({ ...productForm, initialStock: event.target.value })}
              type="number"
              min="0"
              placeholder="10"
              required
            />
          </label>
          <label className="admin-form-wide">
            Descricao
            <textarea
              value={productForm.description}
              onChange={(event) => setProductForm({ ...productForm, description: event.target.value })}
              placeholder="Resumo curto para aparecer na vitrine"
            />
          </label>
          <button disabled={busyAction === 'create-product'}>
            <i className="admin-icon admin-icon-plus" aria-hidden="true" />
            {busyAction === 'create-product' ? 'Cadastrando...' : 'Cadastrar produto'}
          </button>
        </form>
      </Panel>

      <Panel title="Editar produto" panelRef={editProductRef}>
        <form className="admin-commerce-form" onSubmit={submitProductEdit}>
          <label>
            ID
            <input
              value={productEditForm.id}
              onChange={(event) => setProductEditForm({ ...productEditForm, id: event.target.value })}
              type="number"
              min="1"
              placeholder="ID do produto"
              required
            />
          </label>
          <label>
            Categoria
            <select
              value={productEditForm.categoryName}
              onChange={(event) => setProductEditForm({ ...productEditForm, categoryName: event.target.value })}
              required
            >
              <option value="">Selecione</option>
              {categories.map((category) => (
                <option key={category.id} value={category.name}>{category.name}</option>
              ))}
            </select>
          </label>
          <label>
            Nome
            <input
              value={productEditForm.name}
              onChange={(event) => setProductEditForm({ ...productEditForm, name: event.target.value })}
              required
            />
          </label>
          <label>
            Preco
            <input
              value={productEditForm.price}
              onChange={(event) => setProductEditForm({ ...productEditForm, price: event.target.value })}
              type="number"
              min="0"
              step="0.01"
              required
            />
          </label>
          <label className="admin-form-wide">
            Descricao
            <textarea
              value={productEditForm.description}
              onChange={(event) => setProductEditForm({ ...productEditForm, description: event.target.value })}
            />
          </label>
          <button disabled={busyAction === 'edit-product'}>
            <i className="admin-icon admin-icon-edit" aria-hidden="true" />
            {busyAction === 'edit-product' ? 'Salvando...' : 'Salvar alteracoes'}
          </button>
        </form>
      </Panel>

      <Panel title="Estoque e status" panelRef={stockPanelRef}>
        <form className="admin-commerce-form admin-stock-form" onSubmit={restockProduct}>
          <label>
            Produto
            {products.length ? (
              <select
                value={stockAction.productId}
                onChange={(event) => setStockAction({ ...stockAction, productId: event.target.value })}
                required
              >
                <option value="">Selecione</option>
                {products.map((product) => (
                  <option key={product.id} value={product.id}>
                    #{product.id} - {product.name}
                  </option>
                ))}
              </select>
            ) : (
              <input
                value={stockAction.productId}
                onChange={(event) => setStockAction({ ...stockAction, productId: event.target.value })}
                type="number"
                min="1"
                placeholder="ID do produto"
                required
              />
            )}
          </label>
          <label>
            Quantidade
            <input
              value={stockAction.quantity}
              onChange={(event) => setStockAction({ ...stockAction, quantity: event.target.value })}
              type="number"
              min="1"
              placeholder="5"
              required
            />
          </label>
          <button disabled={busyAction === 'restock'}>
            <i className="admin-icon admin-icon-stock" aria-hidden="true" />
            {busyAction === 'restock' ? 'Repondo...' : 'Repor estoque'}
          </button>
          <button
            type="button"
            className={selectedStockProduct(products, stockAction.productId)?.active === false ? '' : 'danger-button'}
            disabled={!stockAction.productId || busyAction === `deactivate-${stockAction.productId}` || busyAction === `activate-${stockAction.productId}`}
            onClick={() => {
              const product = selectedStockProduct(products, stockAction.productId)

              if (product) {
                void toggleProductStatus(product)
              } else {
                void deactivateProduct(stockAction.productId)
              }
            }}
          >
            {selectedStockProduct(products, stockAction.productId)?.active === false ? 'Ativar ID' : 'Desativar ID'}
          </button>
        </form>

        <div className="admin-product-strip">
          {lowStockProducts.slice(0, 4).map((product) => (
            <article key={product.id}>
              <img src={placeholderImageForCategory(product.categoryName)} alt="" loading="lazy" decoding="async" />
              <div>
                <strong>{product.name}</strong>
                <span>{money(product.price)} · {getAvailableStock(product)} em estoque</span>
              </div>
              <button type="button" onClick={() => goToProductEdit(product)}>Editar</button>
              <button type="button" onClick={() => goToRestock(product)}>Repor</button>
              <button
                type="button"
                className={product.active === false ? '' : 'danger-button'}
                onClick={() => toggleProductStatus(product)}
                disabled={busyAction === `deactivate-${product.id}` || busyAction === `activate-${product.id}`}
              >
                {product.active === false ? 'Ativar' : 'Desativar'}
              </button>
            </article>
          ))}
          {!lowStockProducts.length ? (
            <p className="empty-state">Nenhum produto em baixo estoque no momento.</p>
          ) : null}
        </div>
      </Panel>

      <Panel title="Categorias">
        <form className="admin-commerce-form" onSubmit={submitCategory}>
          <label>
            ID
            <input
              value={categoryForm.id}
              onChange={(event) => setCategoryForm({ ...categoryForm, id: event.target.value })}
              type="number"
              min="1"
              placeholder="Novo"
            />
          </label>
          <label>
            Nome
            <input
              value={categoryForm.name}
              onChange={(event) => setCategoryForm({ ...categoryForm, name: event.target.value })}
              placeholder="Nome da categoria"
              required
            />
          </label>
          <label className="admin-form-wide">
            Descricao
            <textarea
              value={categoryForm.description}
              onChange={(event) => setCategoryForm({ ...categoryForm, description: event.target.value })}
              placeholder="Detalhe curto da categoria"
            />
          </label>
          <button disabled={busyAction === 'save-category'}>
            <i className="admin-icon admin-icon-tag" aria-hidden="true" />
            {categoryForm.id ? 'Atualizar categoria' : 'Criar categoria'}
          </button>
        </form>

        <div className="admin-category-list">
          {categories.map((category) => (
            <article key={category.id}>
              <div>
                <strong>{category.name}</strong>
                <small>{category.description || 'Sem descricao cadastrada'}</small>
              </div>
              <button type="button" onClick={() => fillCategoryEdit(category)}>Editar</button>
              <button
                type="button"
                className="danger-button"
                onClick={() => deleteCategory(category.id)}
                disabled={busyAction === `delete-category-${category.id}`}
              >
                Remover
              </button>
            </article>
          ))}
        </div>
      </Panel>

      <TablePanel<EmailLog>
        title="Log de E-mails Enviados"
        rows={emails}
        columns={['id', 'toEmail', 'subject', 'type', 'status', 'createdAt']}
      />
    </div>
  )
}

function toProductPayload(product: ProductForm) {
  return {
    name: product.name,
    description: product.description,
    price: Number(product.price),
    initialStock: Number(product.initialStock || 0),
    categoryName: product.categoryName
  }
}

function getAvailableStock(product: Product) {
  return product.availableStock ?? product.avaliableStock ?? 0
}

function getTotalStock(product: Product) {
  return product.totalStock ?? getAvailableStock(product)
}

function getReservedStock(product: Product) {
  return product.reservedStock ?? 0
}

function selectedStockProduct(products: Product[], productId: string) {
  return products.find((product) => String(product.id) === productId)
}

function scrollToSection(section: HTMLElement | null) {
  window.setTimeout(() => {
    section?.scrollIntoView({
      behavior: 'smooth',
      block: 'start'
    })
  }, 0)
}
