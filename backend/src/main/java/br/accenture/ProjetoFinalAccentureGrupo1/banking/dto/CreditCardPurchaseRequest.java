package br.accenture.ProjetoFinalAccentureGrupo1.banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreditCardPurchaseRequest(
        @NotNull(message = "O valor da compra e obrigatorio")
        @DecimalMin(value = "0.01", message = "O valor da compra deve ser maior que zero")
        BigDecimal amount,

        @NotBlank(message = "O nome do estabelecimento e obrigatorio")
        @Size(max = 120, message = "O nome do estabelecimento deve ter no maximo 120 caracteres")
        String merchantName,

        @NotBlank(message = "A descricao da compra e obrigatoria")
        @Size(max = 255, message = "A descricao deve ter no maximo 255 caracteres")
        String description,

        @Size(max = 120, message = "A referencia deve ter no maximo 120 caracteres")
        String reference
) {}
