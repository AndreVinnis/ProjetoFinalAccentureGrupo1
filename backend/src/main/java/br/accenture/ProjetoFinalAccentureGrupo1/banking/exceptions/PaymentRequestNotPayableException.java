package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

public class PaymentRequestNotPayableException extends RuntimeException {
    public PaymentRequestNotPayableException(String code, String statusReason) {
        super("Cobrança " + code + " não pode ser paga. Status: " + statusReason);
    }
}