package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

public class InvalidCardException extends RuntimeException {
    public InvalidCardException(String message) {
        super("Dados do cartão inválido: " + message);
    }
}
