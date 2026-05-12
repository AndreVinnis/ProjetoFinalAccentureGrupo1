package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions;

public class CartWasNotClosedException extends RuntimeException {
    public CartWasNotClosedException(Long id) {
        super("O carrinho não foi fechado: Carrinho id: " + id);
    }
}
