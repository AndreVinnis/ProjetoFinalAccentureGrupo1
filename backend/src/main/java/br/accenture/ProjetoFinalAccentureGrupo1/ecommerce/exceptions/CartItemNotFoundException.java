package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(Long productId) {
        super("Item não encontrado no carrinho para o produto: " + productId);
    }
}
