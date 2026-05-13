package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions;

public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(String name) {
        super("Nenhuma categoria encontrada com essa nome: " + name);
    }
}
