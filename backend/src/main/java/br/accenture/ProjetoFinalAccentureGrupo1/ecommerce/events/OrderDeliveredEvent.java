package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events;

import java.time.Instant;

// transição SHIPPED → DELIVERED, 5 dias após o envio
// Autor: André Vinícius Barros Macambira
public record OrderDeliveredEvent(
        Long orderId,
        Long userId,
        String customerName,
        String customerEmail,
        Instant deliveredAt
) {}
