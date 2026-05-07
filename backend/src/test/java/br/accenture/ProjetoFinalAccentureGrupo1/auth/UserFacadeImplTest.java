package br.accenture.ProjetoFinalAccentureGrupo1.auth;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserFacadeImpl;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserInfo;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.domain.User;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.enums.Role;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.exceptions.UserNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserFacadeImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserFacadeImpl userFacade;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Ana Silva")
                .email("ana@email.com")
                .cpf("12345678901")
                .birthDate(LocalDate.of(1990, 5, 15))
                .roles(Set.of(Role.CUSTOMER))
                .active(true)
                .build();
    }

    @Test
    void findById_DeveRetornarUserInfo_QuandoUsuarioExiste() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserInfo info = userFacade.findById(1L);

        assertNotNull(info);
        assertEquals(1L, info.id());
        assertEquals("Ana Silva", info.name());
        assertEquals("ana@email.com", info.email());
        assertEquals("12345678901", info.cpf());
        assertEquals(LocalDate.of(1990, 5, 15), info.birthDate());
        assertEquals(Set.of(Role.CUSTOMER), info.roles());
    }

    @Test
    void findById_DeveLancarException_QuandoUsuarioNaoExiste() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(
                UserNotFoundException.class,
                () -> userFacade.findById(99L)
        );
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void exists_DeveRetornarTrue_QuandoUsuarioExiste() {
        when(userRepository.existsById(1L)).thenReturn(true);
        assertTrue(userFacade.exists(1L));
    }

    @Test
    void exists_DeveRetornarFalse_QuandoUsuarioNaoExiste() {
        when(userRepository.existsById(99L)).thenReturn(false);
        assertFalse(userFacade.exists(99L));
    }

    @Test
    void hasRole_DeveRetornarTrue_QuandoUsuarioPossuiAQuelaRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        assertTrue(userFacade.hasRole(1L, Role.CUSTOMER));
    }

    @Test
    void hasRole_DeveRetornarFalse_QuandoUsuarioNaoPossuiAQuelaRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        assertFalse(userFacade.hasRole(1L, Role.BANKING_ADMIN));
    }

    @Test
    void hasRole_DeveRetornarFalse_QuandoUsuarioNaoExiste() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertFalse(userFacade.hasRole(99L, Role.CUSTOMER));
    }
}
