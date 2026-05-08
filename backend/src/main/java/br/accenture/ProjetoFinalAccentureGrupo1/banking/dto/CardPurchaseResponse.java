package br.accenture.ProjetoFinalAccentureGrupo1.banking.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CardPurchaseResponse(
        Long id,
        Long invoiceId,
        BigDecimal amount,
        String description,
        String reference,
        Instant purchaseDate
) {}
