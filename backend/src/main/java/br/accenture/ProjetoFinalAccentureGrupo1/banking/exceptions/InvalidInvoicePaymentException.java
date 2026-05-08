package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

public class InvalidInvoicePaymentException extends RuntimeException {

    public InvalidInvoicePaymentException(String message) {
        super(message);
    }
}
