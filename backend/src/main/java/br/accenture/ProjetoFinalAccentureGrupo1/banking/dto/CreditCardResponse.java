package br.accenture.ProjetoFinalAccentureGrupo1.banking.dto;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.CreditCardStatus;

import java.math.BigDecimal;

public record CreditCardResponse(
        Long id,
        String holderName,
        String cardNumbers,
        String cvv,
        int expirationMonth,
        int expirationYear,
        CreditCardStatus status,
        BigDecimal creditLimit,
        BigDecimal availableLimit
) {}
