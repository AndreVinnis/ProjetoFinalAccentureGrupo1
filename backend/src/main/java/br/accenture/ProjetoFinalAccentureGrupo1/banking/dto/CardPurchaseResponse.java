package br.accenture.ProjetoFinalAccentureGrupo1.banking.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CardPurchaseResponse(
        Long id,
        Long invoiceId,
        BigDecimal amount,
        String description,
        String reference,
        Integer installmentNumber,
        Integer installmentTotal,
        Instant purchaseDate
) {
    public CardPurchaseResponse(
            Long id,
            Long invoiceId,
            BigDecimal amount,
            String description,
            String reference,
            Instant purchaseDate
    ) {
        this(id, invoiceId, amount, description, reference, 1, 1, purchaseDate);
    }
}
