package br.accenture.ProjetoFinalAccentureGrupo1.banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PayInvoiceRequest(
        @NotNull
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        BigDecimal amount
) {}
