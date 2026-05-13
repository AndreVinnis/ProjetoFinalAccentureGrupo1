package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank String name,
        @NotBlank String description
) {
}
