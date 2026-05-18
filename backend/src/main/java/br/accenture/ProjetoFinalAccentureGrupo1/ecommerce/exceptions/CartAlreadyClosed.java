package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions;

public class CartAlreadyClosed extends RuntimeException {
    public CartAlreadyClosed(Long id) {
        super("Sua sacola está fechada. Reabra o carrinho para alterar os produtos.");
    }
}
