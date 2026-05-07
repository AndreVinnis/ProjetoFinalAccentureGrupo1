package br.accenture.ProjetoFinalAccentureGrupo1.auth.dto;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.enums.Role;
import java.util.Set;

// Autor: André Vinícius Barros Macambira
public record AuthResponse(
    String token,
    Long userId,
    String name,
    Set<Role> roles
) {
}
