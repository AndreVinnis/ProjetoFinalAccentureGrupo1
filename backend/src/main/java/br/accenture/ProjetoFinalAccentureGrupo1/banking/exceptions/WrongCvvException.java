package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

public class WrongCvvException extends RuntimeException {
    public WrongCvvException() {
        super("O CVV informado está errado");
    }
}
