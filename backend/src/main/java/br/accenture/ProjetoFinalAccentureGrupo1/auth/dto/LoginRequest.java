package br.accenture.ProjetoFinalAccentureGrupo1.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Autor: André Vinícius Barros Macambira
public record LoginRequest(

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    String email,

    @NotBlank(message = "Senha é obrigatória")
    String password
) {}
