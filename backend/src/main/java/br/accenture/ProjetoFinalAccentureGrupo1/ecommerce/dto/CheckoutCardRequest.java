package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckoutCardRequest(
        @NotNull Long savedCardId,
        @NotBlank String cvv
) {}
