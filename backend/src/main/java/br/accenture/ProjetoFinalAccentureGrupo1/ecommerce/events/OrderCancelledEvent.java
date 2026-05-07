package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events;

import java.math.BigDecimal;
import java.time.Instant;

/*
 * Publicado quando um pedido é cancelado.
 * Se o pedido estava PAID, refundIssued = true (estorno foi emitido).
 * Se estava PENDING (PIX não confirmado), refundIssued = false.
 *
 * Autor: André Vinícius Barros Macambira
 */
public record OrderCancelledEvent(
        Long orderId,
        Long userId,
        String customerName,
        String customerEmail,
        BigDecimal totalAmount,
        boolean refundIssued,
        Instant cancelledAt
) {}