package br.accenture.ProjetoFinalAccentureGrupo1.auth.exceptions;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("Usuário não encontrado: " + userId);
    }
}
