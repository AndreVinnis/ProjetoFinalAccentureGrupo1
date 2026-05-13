package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions;

public class CategoryAlreadyExistsException extends RuntimeException {

    public CategoryAlreadyExistsException(String name) {
        super("Categoria ja cadastrada: " + name);
    }
}
