package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Long categoryId) {
        super("Categoria nao encontrada: " + categoryId);
    }
}
