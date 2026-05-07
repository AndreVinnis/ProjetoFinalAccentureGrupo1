package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

public class InsufficientCreditLimitException extends RuntimeException {

    public InsufficientCreditLimitException() {
        super("Limite de credito insuficiente");
    }
}
