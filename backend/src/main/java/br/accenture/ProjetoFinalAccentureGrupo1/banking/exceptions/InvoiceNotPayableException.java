package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.InvoiceStatus;

public class InvoiceNotPayableException extends RuntimeException {
    public InvoiceNotPayableException(Long id, InvoiceStatus status) {
        super("Fatura id: " + id + " está: " + status);
    }
}
