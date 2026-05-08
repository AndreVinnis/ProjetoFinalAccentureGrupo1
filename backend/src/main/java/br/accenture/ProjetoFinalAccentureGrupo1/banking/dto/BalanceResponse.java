package br.accenture.ProjetoFinalAccentureGrupo1.banking.dto;

import java.math.BigDecimal;

public record BalanceResponse(
        BigDecimal balance
) {}
