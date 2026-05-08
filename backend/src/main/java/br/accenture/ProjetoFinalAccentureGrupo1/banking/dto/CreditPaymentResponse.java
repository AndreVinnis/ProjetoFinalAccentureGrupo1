package br.accenture.ProjetoFinalAccentureGrupo1.banking.dto;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.CreditCardTransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record CreditPaymentResponse(
        Long transactionId,
        BigDecimal amount,
        Integer installments,
        BigDecimal installmentAmount,
        String merchantName,
        String description,
        CreditCardTransactionStatus status,
        String declineReason,
        BigDecimal remainingLimit,
        BigDecimal invoiceBalance,
        Instant authorizedAt
) {}
