package br.accenture.ProjetoFinalAccentureGrupo1.banking.dto;

import jakarta.validation.constraints.NotBlank;

// Autor: André Vinícius Barros Macambira
public record PayPixRequest(
        @NotBlank(message = "Código é obrigatório")
        String code
) {}
