package br.accenture.ProjetoFinalAccentureGrupo1.banking.events;

import java.time.Instant;

/*
 * Publicado quando uma cobrança PIX expira sem ser paga (após 30 minutos).
 * O ecommerce escuta pra cancelar o pedido (PENDING → CANCELLED).
 * Autor: André Vinícius Barros Macambira
 */
public record PaymentExpiredEvent(
        String reference,
        Instant expiredAt
) {}