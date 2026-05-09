package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(BigDecimal balance, BigDecimal required) {
        super("Saldo insuficiente. Disponível: R$ " + balance + ", necessário: R$ " + required);
    }
}
