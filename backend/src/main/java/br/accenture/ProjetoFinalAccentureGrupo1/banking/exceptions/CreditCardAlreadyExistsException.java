package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

public class CreditCardAlreadyExistsException extends RuntimeException {

    public CreditCardAlreadyExistsException(Long userId) {
        super("Usuario ja possui cartao de credito: " + userId);
    }
}
