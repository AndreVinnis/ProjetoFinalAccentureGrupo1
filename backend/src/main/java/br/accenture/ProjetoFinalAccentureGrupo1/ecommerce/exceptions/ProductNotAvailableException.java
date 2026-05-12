package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions;

public class ProductNotAvailableException extends RuntimeException {

    public ProductNotAvailableException(String message) {
        super(message);
    }
}
