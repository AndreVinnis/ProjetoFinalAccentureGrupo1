package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(Long userId) {
        super("Conta bancaria nao encontrada para o usuario: " + userId);
    }
}
