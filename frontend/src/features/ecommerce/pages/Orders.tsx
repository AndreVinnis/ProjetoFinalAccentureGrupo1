import { useCallback, useEffect, useState } from 'react'
import { TablePanel } from '../../../components/ui/Table'
import type { ApiClient } from '../../../services/api'
import type { Order } from '../types/order'

export function Orders({ api }: { api: ApiClient }) {
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)

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
    console.log(
      'Visualizar pedido',
      orderId
    )

    await api.get(
      `/ecommerce/orders/${orderId}`
    )
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
    </div>
  )
}