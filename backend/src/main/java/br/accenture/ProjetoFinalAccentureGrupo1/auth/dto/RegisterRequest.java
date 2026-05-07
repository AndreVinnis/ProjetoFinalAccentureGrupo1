package br.accenture.ProjetoFinalAccentureGrupo1.auth.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record RegisterRequest(

        // Dados do User (módulo auth)
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String name,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 150)
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres")
        String password,

        @NotBlank(message = "CPF é obrigatório")
        @Size(min = 11, max = 14, message = "CPF inválido")
        String cpf,

        @NotNull(message = "Data de nascimento é obrigatória")
        @Past(message = "Data de nascimento deve ser no passado")
        LocalDate birthDate,

        // Dados do Customer (módulo ecommerce)
        @NotBlank(message = "Telefone é obrigatório")
        @Size(max = 20)
        String phone,

        @NotBlank(message = "CEP é obrigatório")
        @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP inválido")
        String zipCode,

        @NotBlank(message = "Estado é obrigatório")
        @Size(min = 2, max = 2, message = "Estado deve ter 2 caracteres (UF)")
        String state,

        @NotBlank(message = "Cidade é obrigatória")
        @Size(max = 100)
        String city,

        @NotBlank(message = "Bairro é obrigatório")
        @Size(max = 100)
        String neighborhood,

        @NotBlank(message = "Rua é obrigatória")
        @Size(max = 150)
        String street,

        @NotBlank(message = "Número é obrigatório")
        @Size(max = 10)
        String number,

        @Size(max = 100, message = "Complemento deve ter no máximo 100 caracteres")
        String complement
) {}
