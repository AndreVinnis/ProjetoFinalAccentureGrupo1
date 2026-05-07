package br.accenture.ProjetoFinalAccentureGrupo1.auth.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.domain.User;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// Autor: André Vinícius Barros Macambira
class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    public void setup(){
        String secret = "cHJvamV0by1maW5hbC1hY2FkZW1pYS1qYXZhLXNlY3JldC1ub3QtZm9yLXByb2R1Y3Rpb24xMjM=";
        long expirationMinutes = 60L;
        jwtService = new JwtService(secret, expirationMinutes);

        user = User.builder()
                .id(1L)
                .name("Ana")
                .email("ana@email.com")
                .roles(Set.of(Role.CUSTOMER))
                .build();
    }

    @Test
    void testGenerateToken_WhenPassCorrectData() {
        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isValid(token));
        assertEquals("ana@email.com", jwtService.extractEmail(token));
        assertEquals(1L, jwtService.extractUserId(token));
        assertEquals(Set.of(Role.CUSTOMER), jwtService.extractRoles(token));
    }

    @Test
    void testIsValid_WhenTokenIsTampered() {
        String token = jwtService.generateToken(user);
        String tampered = token + "XZ";

        assertFalse(jwtService.isValid(tampered));
    }
}