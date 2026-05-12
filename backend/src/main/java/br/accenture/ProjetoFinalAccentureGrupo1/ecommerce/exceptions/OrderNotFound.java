package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions;

public class OrderNotFound extends RuntimeException {
    public OrderNotFound(Long id) {
        super("O pedido nao foi encontrado. Pedido id: " + id);
    }
}
