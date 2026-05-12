package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums;

/*
 * Estado atual de um Order
 *
 * Transições válidas:
 *   PENDING ──pagamento PIX confirmado──▶ PAID ──+1 dia──▶ SHIPPED ──+5 dias──▶ DELIVERED
 *      │                                  │
 *      │                                  └──cancelamento manual──▶ CANCELLED (com estorno)
 *      │
 *      ├──pagamento PIX expirou (30 min)──▶ CANCELLED
 *      └──erro no banking──▶ FAILED
 *
 * Pedidos pagos no cartão pulam PENDING e nascem direto como PAID (o débito é síncrono).
 * Pedidos PIX nascem em PENDING e só viram PAID quando o PaymentReceivedEvent chega do banking.
 *
 * Autor: André Vinícius Barros Macambira
 */
public enum OrderStatus {

    PENDING,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    FAILED
}
