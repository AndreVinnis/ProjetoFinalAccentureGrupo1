package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException(Long cardId) {
        super("Cartao nao encontrada para o cartao: " + cardId);
    }
}
