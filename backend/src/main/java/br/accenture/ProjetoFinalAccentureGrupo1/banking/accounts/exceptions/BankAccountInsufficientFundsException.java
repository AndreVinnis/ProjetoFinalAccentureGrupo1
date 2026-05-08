package br.accenture.ProjetoFinalAccentureGrupo1.banking.accounts.exceptions;

public class BankAccountInsufficientFundsException extends RuntimeException {

    public BankAccountInsufficientFundsException(String message) {
        super(message);
    }
}
