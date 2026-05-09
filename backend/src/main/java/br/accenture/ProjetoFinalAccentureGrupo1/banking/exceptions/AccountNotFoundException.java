package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(Long userId) {
        super("Conta não encontrada para o usuário: " + userId);
    }
}
