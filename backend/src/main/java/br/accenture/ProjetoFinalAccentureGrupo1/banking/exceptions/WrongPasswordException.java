package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

public class WrongPasswordException extends RuntimeException {
    public WrongPasswordException() {
        super("A senha informado está incorreta");
    }
}
