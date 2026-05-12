package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions;

public class OrderShippedException extends RuntimeException {
    public OrderShippedException(Long id) {
        super("Não é possível cancelar um pedido já enviado. Pedido id: " + id);
    }
}
