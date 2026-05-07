package br.accenture.ProjetoFinalAccentureGrupo1.auth.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.dto.AuthResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.dto.LoginRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.dto.RegisterRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.enums.Role;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.events.UserRegisteredEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.exceptions.CpfAlreadyExistsException;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.exceptions.EmailAlreadyExistsException;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.domain.User;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new EmailAlreadyExistsException(req.email());
        }
        if (userRepository.existsByCpf(req.cpf())) {
            throw new CpfAlreadyExistsException(req.cpf());
        }

        User user = User.builder()
                .name(req.name())
                .email(req.email())
                .cpf(req.cpf())
                .birthDate(req.birthDate())
                .passwordHash(passwordEncoder.encode(req.password()))
                .roles(Set.of(Role.CUSTOMER))
                .active(true)
                .build();

        User saved = userRepository.save(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                saved.getCpf(),
                saved.getBirthDate(),
                req.phone(),
                req.zipCode(),
                req.state(),
                req.city(),
                req.neighborhood(),
                req.street(),
                req.number(),
                req.complement()
        ));

        String token = jwtService.generateToken(saved);
        return new AuthResponse(token, saved.getId(), saved.getName(), saved.getRoles());
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.email(), req.password()));

        User user = userRepository.findByEmail(req.email()).orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getName(), user.getRoles());
    }
}