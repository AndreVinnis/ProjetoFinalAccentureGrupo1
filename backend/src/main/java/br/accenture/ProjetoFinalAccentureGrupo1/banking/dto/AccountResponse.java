package br.accenture.ProjetoFinalAccentureGrupo1.banking.dto;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountType;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        Long id,
        Long userId,
        String accountNumber,
        BigDecimal balance,
        AccountType accountType,
        AccountStatus status,
        Instant createdAt
) {}
