package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions;

public class IllegalOrderStatusException extends RuntimeException {
    public IllegalOrderStatusException(String status) {
        super("Só é possível cancelar um pedido antes de ser enviado. Status atual: " + status);
    }
}
