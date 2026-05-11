package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

public class InvoiceNotFoundException extends RuntimeException {

    public InvoiceNotFoundException(Long id) {
        super("Fatura não encontrada. Fatura id: " + id);
    }
}
