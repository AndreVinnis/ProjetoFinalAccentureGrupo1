package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
