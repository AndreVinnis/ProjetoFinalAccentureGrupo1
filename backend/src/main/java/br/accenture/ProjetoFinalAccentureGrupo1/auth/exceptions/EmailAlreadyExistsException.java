package br.accenture.ProjetoFinalAccentureGrupo1.auth.exceptions;

// Autor: André Vinícius Barros Macambira
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("E-mail já cadastrado: " + email);
    }
}
