package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events;

import java.math.BigDecimal;
import java.time.Instant;

// Autor: André Vinícius Barros Macambira
public record OrderPaidEvent(
        Long orderId,
        Long userId,
        String customerName,
        String customerEmail,
        BigDecimal totalAmount,
        String paymentMethod,
        Instant paidAt
) {}
