package br.accenture.ProjetoFinalAccentureGrupo1.auth.api;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.enums.Role;
import java.time.LocalDate;
import java.util.Set;

// Autor: André Vinícius Barros Macambira
public record UserInfo(
        Long id,
        String name,
        String email,
        String cpf,
        LocalDate birthDate,
        Set<Role> roles
) {}
