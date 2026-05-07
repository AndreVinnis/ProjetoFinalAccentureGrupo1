package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

public class CreditCardBlockedException extends RuntimeException {

    public CreditCardBlockedException() {
        super("Cartao de credito bloqueado");
    }
}
