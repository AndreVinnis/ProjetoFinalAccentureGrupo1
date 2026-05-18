package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CheckoutCardRequest(
        @NotNull Long savedCardId,
        @NotBlank String cvv,
        @Min(1) @Max(12) Integer installments
) {
    public CheckoutCardRequest(Long savedCardId, String cvv) {
        this(savedCardId, cvv, 1);
    }

    public int installmentsOrDefault() {
        return installments == null ? 1 : installments;
    }
}
