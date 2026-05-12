package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto;

import java.time.Instant;

public record SavedCardResponse(
        Long id,
        String last4Digits,
        String holderName,
        Instant createdAt
) {}
