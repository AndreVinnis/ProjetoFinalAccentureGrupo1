package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions;

public class CartAlreadyClosed extends RuntimeException {
    public CartAlreadyClosed(Long id) {
        super("O carrinho está fechado: Carrinho id: " + id);
    }
}
