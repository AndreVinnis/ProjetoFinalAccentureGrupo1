package br.accenture.ProjetoFinalAccentureGrupo1.banking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PixRequest(
        @NotBlank(message = "O endereço do destinatário é obrigatório")
        String recipientEmail,
        @NotNull(message = "Digite o valor da transação")
        BigDecimal amount,
        String description,
        @NotBlank(message = "A senha é obrigatória")
        String password
) {
}
