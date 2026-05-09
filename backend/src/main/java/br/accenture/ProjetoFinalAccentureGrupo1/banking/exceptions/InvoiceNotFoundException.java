package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

public class InvoiceNotFoundException extends RuntimeException {

    public InvoiceNotFoundException(Long invoiceId) {
        super("Fatura nao encontrada: " + invoiceId);
    }
}
