package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto;

import java.math.BigDecimal;

public record DiscountApplication(
        String ruleName,
        String description,
        BigDecimal discountAmount
) {
}
