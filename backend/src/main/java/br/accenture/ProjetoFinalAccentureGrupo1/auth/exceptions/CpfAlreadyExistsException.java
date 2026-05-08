package br.accenture.ProjetoFinalAccentureGrupo1.auth.exceptions;

// Autor: André Vinícius Barros Macambira
public class CpfAlreadyExistsException extends RuntimeException {
    public CpfAlreadyExistsException(String cpf) {
        super("CPF já cadastrado: " + cpf);
    }
}
