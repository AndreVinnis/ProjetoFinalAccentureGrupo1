package br.accenture.ProjetoFinalAccentureGrupo1.banking.dto;

import java.math.BigDecimal;

public record CreditLimitResponse(
        BigDecimal creditLimit,
        BigDecimal availableLimit,
        BigDecimal usedLimit
) {}
