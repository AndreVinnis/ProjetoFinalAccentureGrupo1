package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

public class CreditCardNotFoundException extends RuntimeException {

    public CreditCardNotFoundException(Long userId) {
        super("Cartao de credito nao encontrado para o usuario: " + userId);
    }
}
