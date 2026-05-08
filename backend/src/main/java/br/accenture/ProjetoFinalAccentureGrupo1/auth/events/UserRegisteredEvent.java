package br.accenture.ProjetoFinalAccentureGrupo1.auth.events;

import java.time.LocalDate;

// Autor: André Vinícius Barros Macambira
public record UserRegisteredEvent(
        Long userId,
        String name,
        String email,
        String cpf,
        LocalDate birthDate,
        String phone,
        String zipCode,
        String state,
        String city,
        String neighborhood,
        String street,
        String number,
        String complement
) {}
