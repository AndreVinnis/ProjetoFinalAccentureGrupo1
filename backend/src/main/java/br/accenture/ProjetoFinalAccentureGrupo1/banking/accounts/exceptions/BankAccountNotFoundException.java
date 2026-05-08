package br.accenture.ProjetoFinalAccentureGrupo1.banking.accounts.exceptions;

public class BankAccountNotFoundException extends RuntimeException {

    public BankAccountNotFoundException(String message) {
        super(message);
    }
}
