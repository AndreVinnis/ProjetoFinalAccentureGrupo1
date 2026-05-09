package br.accenture.ProjetoFinalAccentureGrupo1.banking.dto;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.PaymentRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.PaymentRequestStatus;
import java.math.BigDecimal;
import java.time.Instant;

// Autor: André Vinícius Barros Macambira
public record PaymentRequestResponse(
        String code,
        BigDecimal amount,
        String description,
        String reference,
        PaymentRequestStatus status,
        Instant createdAt,
        Instant expiresAt,
        Instant paidAt
) {

    public static PaymentRequestResponse from(PaymentRequest request) {
        return new PaymentRequestResponse(
                request.getCode(),
                request.getAmount(),
                request.getDescription(),
                request.getReference(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getExpiresAt(),
                request.getPaidAt()
        );
    }
}