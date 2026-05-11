package br.accenture.ProjetoFinalAccentureGrupo1.shared.exceptions;

public class CryptographyException extends RuntimeException {
    public CryptographyException() {
        super("Falha na criptografia");
    }
}
