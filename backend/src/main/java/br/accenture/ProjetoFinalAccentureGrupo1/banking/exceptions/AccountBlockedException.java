package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

public class AccountBlockedException extends RuntimeException {

    public AccountBlockedException() {
        super("Conta bloqueada");
    }
}
