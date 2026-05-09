package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

public class PaymentRequestNotFoundException extends RuntimeException {
    public PaymentRequestNotFoundException(String code) {
        super("Cobrança PIX não encontrada para o código: " + code);
    }
}