package br.accenture.ProjetoFinalAccentureGrupo1.banking.dto;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.CreditCardTransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record CreditCardTransactionResponse(
        Long id,
        BigDecimal amount,
        Integer installments,
        BigDecimal installmentAmount,
        String merchantName,
        String description,
        CreditCardTransactionStatus status,
        String declineReason,
        Instant createdAt
) {}
