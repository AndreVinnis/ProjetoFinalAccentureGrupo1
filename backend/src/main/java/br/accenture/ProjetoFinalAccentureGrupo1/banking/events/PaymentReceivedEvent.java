package br.accenture.ProjetoFinalAccentureGrupo1.banking.events;

import java.math.BigDecimal;
import java.time.Instant;

/*
 * Publicado quando uma cobrança PIX é paga com sucesso.
 * O ecommerce escuta pra confirmar o pedido (PENDING → PAID).
 * Autor: André Vinícius Barros Macambira
 */
public record PaymentReceivedEvent(
        String reference,
        Long payerUserId,
        BigDecimal amount,
        Instant paidAt
) {}
