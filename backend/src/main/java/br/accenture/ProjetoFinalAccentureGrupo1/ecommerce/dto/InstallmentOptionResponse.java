package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto;

import java.math.BigDecimal;

public record InstallmentOptionResponse(
        int installments,
        BigDecimal installmentAmount,
        BigDecimal totalAmount,
        String label
) {}
