package br.accenture.ProjetoFinalAccentureGrupo1.auth.api;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.domain.User;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.enums.Role;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.exceptions.UserNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
// Autor: André Vinícius Barros Macambira
public class UserFacadeImpl implements UserFacade{

    private final UserRepository userRepository;

    @Override
    public UserInfo findById(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return toInfo(user);
    }

    @Override
    public boolean exists(Long userId) {
        return userRepository.existsById(userId);
    }

    @Override
    public boolean hasRole(Long userId, Role role) {
        return userRepository.findById(userId)
                .map(user -> user.getRoles().contains(role))
                .orElse(false);
    }

    @Override
    public UserInfo findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(0L));
        return toInfo(user);
    }

    private UserInfo toInfo(User user) {
        return new UserInfo(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCpf(),
                user.getBirthDate(),
                user.getRoles()
        );
    }
}
