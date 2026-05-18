package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions;

public class CartWasNotClosedException extends RuntimeException {
    public CartWasNotClosedException(Long id) {
        super("Feche o carrinho antes de finalizar o pagamento.");
    }
}
