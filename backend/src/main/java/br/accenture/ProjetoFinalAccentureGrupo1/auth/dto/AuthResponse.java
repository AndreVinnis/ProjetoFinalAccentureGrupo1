package br.accenture.ProjetoFinalAccentureGrupo1.auth.dto;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.enums.Role;
import java.util.Set;

public record AuthResponse(
    String token,
    Long userId,
    String name,
    Set<Role> roles
) {
}
