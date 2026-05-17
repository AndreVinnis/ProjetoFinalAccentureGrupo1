package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.CartStatus;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long cartId,
        List<CartItemResponse> items,
        BigDecimal subtotal,
        CartStatus status
) {}
