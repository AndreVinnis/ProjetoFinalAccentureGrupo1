package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

public class AccountAlreadyExistsException extends RuntimeException {

    public AccountAlreadyExistsException(Long userId) {
        super("Usuario ja possui conta bancaria: " + userId);
    }
}
