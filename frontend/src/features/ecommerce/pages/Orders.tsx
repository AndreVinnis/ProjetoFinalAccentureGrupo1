import { useCallback, useEffect, useState } from 'react'
import '../../../styles/ecommerce/orders.css'
import { TablePanel } from '../../../components/ui/Table'
import { date, money } from '../../../utils/format'
import type { ApiClient } from '../../../services/api'
import type { Order } from '../types/order'

export function Orders({ api }: { api: ApiClient }) {
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null)
  const [orderModalLoading, setOrderModalLoading] = useState(false)

  const fetchOrders = useCallback(async () => {
    try {
      setLoading(true)

      const result = await api.get<Order[]>(
        '/ecommerce/orders'
      )

      setOrders(result ?? [])
    } catch (error) {
      console.error(
        'Erro ao buscar pedidos',
        error
      )
    } finally {
      setLoading(false)
    }
  }, [api])

  useEffect(() => {
    let mounted = true

    async function load() {
      try {
        setLoading(true)

        const result = await api.get<Order[]>(
          '/ecommerce/orders'
        )

        if (mounted) {
          setOrders(result ?? [])
        }
      } catch (error) {
        console.error(
          'Erro ao buscar pedidos',
          error
        )
      } finally {
        if (mounted) {
          setLoading(false)
        }
      }
    }

    void load()

    return () => {
      mounted = false
    }
  }, [api])

  async function cancelOrder(orderId: number) {
    if (
      !window.confirm(
        `Deseja realmente cancelar o pedido #${orderId}?`
      )
    ) {
      return
    }

    try {
      await api.post(
        `/ecommerce/orders/${orderId}/cancel`
      )

      await fetchOrders()
    } catch {
      alert(
        'Não foi possível cancelar o pedido. Verifique o status.'
      )
    }
  }

  async function viewOrder(orderId: number) {
    setSelectedOrder(null)
    setOrderModalLoading(true)

    try {
      const order = await api.get<Order>(
        `/ecommerce/orders/${orderId}`
      )

      setSelectedOrder(order)
    } finally {
      setOrderModalLoading(false)
    }
  }

  function closeOrderModal() {
    setSelectedOrder(null)
    setOrderModalLoading(false)
  }

  return (
    <div className="dashboard-grid ecommerce account-workspace">
      <TablePanel<Order>
        title="Meus Pedidos"
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
          <div
            className="button-row tight"
            style={{ display: 'flex' }}
          >
            <button
              onClick={() =>
                viewOrder(order.orderId)
              }
            >
              Ver
            </button>

            {order.status !== 'CANCELLED' &&
              order.status !== 'CANCELADO' && (
                <button
                  className="danger-button"
                  onClick={() =>
                    cancelOrder(order.orderId)
                  }
                >
                  Cancelar
                </button>
              )}
          </div>
        )}
      />

      {orders.length === 0 && !loading && (
        <div
          className="empty-state"
          style={{
            gridColumn: '1 / -1',
            marginTop: '-16px'
          }}
        >
          Você ainda não possui nenhum pedido.
        </div>
      )}

      {(selectedOrder || orderModalLoading) ? (
        <div className="order-modal-backdrop" role="presentation" onMouseDown={closeOrderModal}>
          <section className="order-modal" role="dialog" aria-modal="true" aria-label="Detalhes do pedido" onMouseDown={(event) => event.stopPropagation()}>
            <button className="modal-close-button" type="button" onClick={closeOrderModal} aria-label="Fechar modal">x</button>
            {orderModalLoading ? (
              <div className="order-modal-loading" role="status">
                <span className="order-modal-loader" aria-hidden="true" />
                <strong>Carregando pedido</strong>
                <small>Buscando os detalhes da sua compra.</small>
              </div>
            ) : selectedOrder ? (
              <>
                <div className="order-modal-header">
                  <span>Pedido #{selectedOrder.orderId}</span>
                  <strong>{selectedOrder.status}</strong>
                </div>

                <div className="order-detail-grid">
                  <div>
                    <span>Pagamento</span>
                    <strong>{selectedOrder.paymentMethod || '--'}</strong>
                  </div>
                  <div>
                    <span>Criado em</span>
                    <strong>{date(selectedOrder.createdAt)}</strong>
                  </div>
                  <div>
                    <span>Subtotal</span>
                    <strong>{money(selectedOrder.subtotal)}</strong>
                  </div>
                  <div>
                    <span>Desconto</span>
                    <strong>{money(selectedOrder.discountTotal)}</strong>
                  </div>
                  <div className="order-total-card">
                    <span>Total</span>
                    <strong>{money(selectedOrder.totalAmount)}</strong>
                  </div>
                </div>

                <div className="order-timeline">
                  <span>Pago: {date(selectedOrder.paidAt)}</span>
                  <span>Enviado: {date(selectedOrder.shippedAt)}</span>
                  <span>Entregue: {date(selectedOrder.deliveredAt)}</span>
                  <span>Cancelado: {date(selectedOrder.cancelledAt)}</span>
                </div>

                <div className="order-items-list">
                  <h3>Itens do pedido</h3>
                  {selectedOrder.items?.length ? selectedOrder.items.map((item) => (
                    <div className="order-item-row" key={item.productId}>
                      <div>
                        <strong>{item.productName}</strong>
                        <small>{item.quantity} x {money(item.unitPrice)}</small>
                      </div>
                      <span>{money(item.lineTotal)}</span>
                    </div>
                  )) : (
                    <p className="empty-state">Nenhum item encontrado para este pedido.</p>
                  )}
                </div>
              </>
            ) : null}
          </section>
        </div>
      ) : null}
    </div>
  )
}
