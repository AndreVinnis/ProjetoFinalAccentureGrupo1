package br.accenture.ProjetoFinalAccentureGrupo1.banking.api;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountStatus;
import java.math.BigDecimal;

// André Vinícius Barros Macambira
public record AccountInfo(
        String accountNumber,
        BigDecimal balance,
        AccountStatus status
) {}
