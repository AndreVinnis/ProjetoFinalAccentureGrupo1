package br.accenture.ProjetoFinalAccentureGrupo1.banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DepositRequest(
        @NotNull
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        BigDecimal amount,

        @Size(max = 255)
        String description
) {}
