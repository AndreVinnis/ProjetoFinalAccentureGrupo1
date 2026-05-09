package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountStatus;

public class AccountNotActiveException extends RuntimeException {
    public AccountNotActiveException(AccountStatus status) {
        super("Operação não permitida. Status atual da conta: " + status);
    }
}
