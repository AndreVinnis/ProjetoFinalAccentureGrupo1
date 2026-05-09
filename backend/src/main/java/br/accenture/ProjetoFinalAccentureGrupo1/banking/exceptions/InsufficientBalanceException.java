package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException() {
        super("Saldo insuficiente");
    }

    public InsufficientBalanceException(BigDecimal balance, BigDecimal required) {
        super("Saldo insuficiente. Disponivel: R$ " + balance + ", necessario: R$ " + required);
    }
}
