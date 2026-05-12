package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions;

public class CartNotFoundException extends RuntimeException {
    public CartNotFoundException() {
        super("Ainda não já itens no seu carrinho");
    }
}
