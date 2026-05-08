package br.accenture.ProjetoFinalAccentureGrupo1.notification.dto;

import br.accenture.ProjetoFinalAccentureGrupo1.notification.domain.EmailLog;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.enums.EmailStatus;
import java.time.Instant;

// Autor: André Vinícius Barros Macambira
public record EmailLogResponse(
        Long id,
        String recipient,
        String subject,
        String type,
        EmailStatus status,
        String errorMessage,
        Instant createdAt,
        Instant sentAt
) {
    public static EmailLogResponse from(EmailLog log) {
        return new EmailLogResponse(
                log.getId(),
                log.getRecipient(),
                log.getSubject(),
                log.getType(),
                log.getStatus(),
                log.getErrorMessage(),
                log.getCreatedAt(),
                log.getSentAt()
        );
    }
}
