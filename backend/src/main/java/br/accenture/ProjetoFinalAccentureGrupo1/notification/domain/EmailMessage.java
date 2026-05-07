package br.accenture.ProjetoFinalAccentureGrupo1.notification.domain;

import java.io.Serializable;

// Autor: André Vinícius Barros Macambira
public record EmailMessage(
        String recipient,
        String subject,
        String body,
        String type
) implements Serializable {}
