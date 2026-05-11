package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.InvoiceStatus;

public class InvoiceNotCloseableException extends RuntimeException {
    public InvoiceNotCloseableException(Long invoiceId, InvoiceStatus status) {
        super("Fatura " + invoiceId + " não pode ser fechada. Status atual: " + status);
    }
}
