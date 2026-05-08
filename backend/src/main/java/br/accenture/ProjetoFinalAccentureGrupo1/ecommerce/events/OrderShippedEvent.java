package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events;

import java.time.Instant;

// transição PAID → SHIPPED, 1 dia após o pagamento
// Autor: André Vinícius Barros Macambira
public record OrderShippedEvent(
        Long orderId,
        Long userId,
        String customerName,
        String customerEmail,
        Instant shippedAt
) {}
