package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException() {
        super("Saldo insuficiente");
    }
}
