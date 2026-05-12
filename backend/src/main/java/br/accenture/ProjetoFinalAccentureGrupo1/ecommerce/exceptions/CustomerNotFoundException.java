package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(Long id) {
        super("Cliente nao encontrado: " + id);
    }

    public static CustomerNotFoundException byUserId(Long userId) {
        return new CustomerNotFoundException(userId);
    }
}
