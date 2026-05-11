package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;

public class CreditCardAlreadyExistsException extends RuntimeException {

    public CreditCardAlreadyExistsException(Account account) {
        super("Já existe um cartao vinculado a essa conta: " + account.getAccountNumber());
    }
}
