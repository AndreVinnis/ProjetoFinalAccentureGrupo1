package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

public class InvalidAmountException extends RuntimeException {

    public InvalidAmountException() {
        super("O valor deve ser maior que zero");
    }
}
