package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.OrderStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long orderId,
        OrderStatus status,
        PaymentMethod paymentMethod,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant paidAt,
        Instant shippedAt,
        Instant deliveredAt,
        Instant cancelledAt
) {}
