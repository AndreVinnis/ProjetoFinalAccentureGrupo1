package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterSavedCardRequest(
        @NotBlank
        @Pattern(regexp = "\\d{13,19}", message = "O numero do cartao deve conter entre 13 e 19 digitos")
        String cardNumber,

        @NotBlank
        @Pattern(regexp = "\\d{3,4}", message = "O CVV deve conter 3 ou 4 digitos")
        String cvv,

        @NotNull
        @Min(1)
        @Max(12)
        Integer expirationMonth,

        @NotNull
        @Min(2000)
        Integer expirationYear,

        @Size(max = 120)
        String holderName
) {}
