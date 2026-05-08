package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

public class AccountRestrictedException extends RuntimeException {

    public AccountRestrictedException() {
        super("Conta restrita para debitos");
    }
}
