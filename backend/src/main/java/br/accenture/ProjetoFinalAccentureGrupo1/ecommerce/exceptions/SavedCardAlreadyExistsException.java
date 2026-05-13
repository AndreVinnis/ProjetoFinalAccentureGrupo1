package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions;

public class SavedCardAlreadyExistsException extends RuntimeException {

    public SavedCardAlreadyExistsException() {
        super("Este cartao ja esta salvo para o cliente");
    }
}
