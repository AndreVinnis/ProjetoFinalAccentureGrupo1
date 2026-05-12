package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions;

public class SavedCardNotFoundException extends RuntimeException {

    public SavedCardNotFoundException(Long savedCardId) {
        super("Cartao salvo nao encontrado: " + savedCardId);
    }
}
