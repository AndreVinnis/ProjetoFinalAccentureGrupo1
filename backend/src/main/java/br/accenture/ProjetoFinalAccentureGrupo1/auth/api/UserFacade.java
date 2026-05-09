package br.accenture.ProjetoFinalAccentureGrupo1.auth.api;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.enums.Role;

// Autor: André Vinícius Barros Macambira
public interface UserFacade {

    UserInfo findById(Long userId);

    boolean exists(Long userId);

    boolean hasRole(Long userId, Role role);

    UserInfo findByEmail(String email);
}
