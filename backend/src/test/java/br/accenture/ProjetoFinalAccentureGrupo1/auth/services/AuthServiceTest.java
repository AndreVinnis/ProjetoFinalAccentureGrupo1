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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Autor: André Vinícius Barros Macambira
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRegisterRequest;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterRequest(
                "Ana Silva",
                "ana@email.com",
                "senha12345",
                "12345678901",
                LocalDate.of(1990, 5, 15),
                "11999999999",
                "01310100",
                "SP",
                "São Paulo",
                "Bela Vista",
                "Av. Paulista",
                "1000",
                "Apto 42"
        );
    }

    @Test
    void register_DeveCriarUsuarioERetornarToken_QuandoDadosSaoValidos() {
        // arrange
        when(userRepository.existsByEmail("ana@email.com")).thenReturn(false);
        when(userRepository.existsByCpf("12345678901")).thenReturn(false);
        when(passwordEncoder.encode("senha12345")).thenReturn("hashed-password");

        User savedUser = User.builder()
                .id(1L)
                .name("Ana Silva")
                .email("ana@email.com")
                .cpf("12345678901")
                .birthDate(LocalDate.of(1990, 5, 15))
                .passwordHash("hashed-password")
                .roles(Set.of(Role.CUSTOMER))
                .active(true)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("jwt-token");

        // act
        AuthResponse response = authService.register(validRegisterRequest);

        // assert
        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals(1L, response.userId());
        assertEquals("Ana Silva", response.name());
        assertEquals(Set.of(Role.CUSTOMER), response.roles());

        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void register_DeveLancarException_QuandoEmailJaExiste() {
        when(userRepository.existsByEmail("ana@email.com")).thenReturn(true);

        EmailAlreadyExistsException ex = assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(validRegisterRequest)
        );
        assertTrue(ex.getMessage().contains("ana@email.com"));

        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void register_DeveLancarException_QuandoCpfJaExiste() {
        when(userRepository.existsByEmail("ana@email.com")).thenReturn(false);
        when(userRepository.existsByCpf("12345678901")).thenReturn(true);

        CpfAlreadyExistsException ex = assertThrows(
                CpfAlreadyExistsException.class,
                () -> authService.register(validRegisterRequest)
        );
        assertTrue(ex.getMessage().contains("12345678901"));

        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void login_DeveRetornarToken_QuandoCredenciaisSaoValidas() {
        LoginRequest request = new LoginRequest("ana@email.com", "senha12345");
        User user = User.builder()
                .id(1L)
                .name("Ana Silva")
                .email("ana@email.com")
                .roles(Set.of(Role.CUSTOMER))
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals(1L, response.userId());
        assertEquals("Ana Silva", response.name());
        assertEquals(Set.of(Role.CUSTOMER), response.roles());
    }

    @Test
    void login_DeveLancarBadCredentialsException_QuandoSenhaEInvalida() {
        LoginRequest request = new LoginRequest("ana@email.com", "senha-errada");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciais inválidas"));

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        verify(jwtService, never()).generateToken(any());
        verify(userRepository, never()).findByEmail(any());
    }
}