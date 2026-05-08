package br.accenture.ProjetoFinalAccentureGrupo1.auth.controller;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.dto.LoginRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
// Autor: André Vinícius Barros Macambira
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest(
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
    void register_DeveRetornar201ComToken_QuandoDadosValidos() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.name").value("Ana Silva"))
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"));
    }

    @Test
    void register_DeveRetornar409_QuandoEmailJaExiste() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        RegisterRequest duplicateEmail = new RegisterRequest(
                "Outro Nome",
                "ana@email.com",
                "outrasenha123",
                "98765432100",
                LocalDate.of(1985, 1, 1),
                "11888888888",
                "04567000",
                "SP",
                "São Paulo",
                "Vila Mariana",
                "Rua Dona Inácia",
                "2000",
                null
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateEmail)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void register_DeveRetornar400_QuandoDadosInvalidos() throws Exception {
        String invalidJson = """
            {
                "name": "",
                "email": "nao-eh-email",
                "password": "abc"
            }
            """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields").exists());
    }

    @Test
    void login_DeveRetornar200ComToken_QuandoCredenciaisValidas() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("ana@email.com", "senha12345");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.name").value("Ana Silva"));
    }

    @Test
    void login_DeveRetornar401_QuandoSenhaIncorreta() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        LoginRequest wrongLogin = new LoginRequest("ana@email.com", "senha-errada");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongLogin)))
                .andExpect(status().isUnauthorized());
    }
}