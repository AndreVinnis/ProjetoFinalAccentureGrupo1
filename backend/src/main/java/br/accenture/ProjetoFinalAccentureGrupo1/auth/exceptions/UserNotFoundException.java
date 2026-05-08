package br.accenture.ProjetoFinalAccentureGrupo1.auth.exceptions;

// Autor: André Vinícius Barros Macambira
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("Usuário não encontrado: " + userId);
    }
}
