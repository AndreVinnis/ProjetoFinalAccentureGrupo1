package br.accenture.ProjetoFinalAccentureGrupo1.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

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

    // Dados do AccountHolder (módulo banking)
    @NotNull(message = "Data de nascimento é obrigatória")
    @Past(message = "Data de nascimento deve ser no passado")
    LocalDate birthDate,

    // Dados do Customer (módulo ecommerce)
    @NotBlank(message = "CPF é obrigatório")
    @Size(min = 11, max = 14, message = "CPF inválido")
    String cpf,


    @NotBlank(message = "Endereço de entrega é obrigatório")
    @Size(max = 255)
    String shippingAddress,

    @NotBlank(message = "Telefone é obrigatório")
    @Size(max = 20)
    String phone
) {}
