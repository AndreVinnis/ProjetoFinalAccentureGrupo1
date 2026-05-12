package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions;

public class CartEmptyException extends RuntimeException {
    public CartEmptyException() {
        super("Seu carrinho esta vazio");
    }
}
