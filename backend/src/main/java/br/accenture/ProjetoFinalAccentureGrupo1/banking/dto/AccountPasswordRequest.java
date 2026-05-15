package br.accenture.ProjetoFinalAccentureGrupo1.banking.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountPasswordRequest(
        @NotBlank
        String password
) {
}
