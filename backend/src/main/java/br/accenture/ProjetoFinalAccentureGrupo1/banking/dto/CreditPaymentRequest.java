package br.accenture.ProjetoFinalAccentureGrupo1.banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreditPaymentRequest(
        @NotNull(message = "O valor do pagamento e obrigatorio")
        @DecimalMin(value = "0.01", message = "O valor do pagamento deve ser maior que zero")
        BigDecimal amount,

        @NotNull(message = "O numero de parcelas e obrigatorio")
        @Min(value = 1, message = "O pagamento deve ter pelo menos 1 parcela")
        @Max(value = 12, message = "O pagamento pode ter no maximo 12 parcelas")
        Integer installments,

        @NotBlank(message = "O nome do estabelecimento e obrigatorio")
        @Size(max = 120, message = "O nome do estabelecimento deve ter no maximo 120 caracteres")
        String merchantName,

        @NotBlank(message = "A descricao do pagamento e obrigatoria")
        @Size(max = 255, message = "A descricao deve ter no maximo 255 caracteres")
        String description
) {}
