package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
        @NotBlank(message = "Endereco de entrega e obrigatorio")
        @Size(max = 500, message = "Endereco de entrega deve ter no maximo 500 caracteres")
        String shippingAddress,

        @NotBlank(message = "Telefone e obrigatorio")
        @Size(max = 20, message = "Telefone deve ter no maximo 20 caracteres")
        String phone
) {}
