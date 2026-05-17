package br.accenture.ProjetoFinalAccentureGrupo1.banking.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.security.JwtAuthenticationFilter;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.AccountPasswordRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CardPurchaseResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CreditCardResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CreditLimitResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.CreditCardStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.CreditCardNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.WrongPasswordException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.CreditCardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CreditCardController.class)
@AutoConfigureMockMvc(addFilters = false)
class CreditCardControllerTest {

    private static final String EMAIL = "ana@email.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreditCardService creditCardService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private CreditCardResponse cardResponse;

    @BeforeEach
    void setUp() {
        cardResponse = new CreditCardResponse(
                1L,
                "Ana Silva",
                "1234567890123456",
                "123",
                5,
                2031,
                CreditCardStatus.ACTIVE,
                new BigDecimal("5000.00"),
                new BigDecimal("4500.00")
        );
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void findMyCard_DeveRetornar200ComCartao() throws Exception {
        when(creditCardService.findMyCard(EMAIL, "1234")).thenReturn(cardResponse);

        AccountPasswordRequest body = new AccountPasswordRequest("1234");

        mockMvc.perform(post("/banking/cards/me")
                        .with(user(EMAIL).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.holderName").value("Ana Silva"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.availableLimit").value(4500.00));

        verify(creditCardService).findMyCard(EMAIL, "1234");
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void findMyCard_DeveRetornar400_QuandoSenhaAusente() throws Exception {
        AccountPasswordRequest body = new AccountPasswordRequest("");

        mockMvc.perform(post("/banking/cards/me")
                        .with(user(EMAIL).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void findMyCard_DeveRetornar400_QuandoSenhaIncorreta() throws Exception {
        // WrongPasswordException é mapeada para 400 no BankingExceptionHandler
        when(creditCardService.findMyCard(EMAIL, "wrong"))
                .thenThrow(new WrongPasswordException());

        AccountPasswordRequest body = new AccountPasswordRequest("wrong");

        mockMvc.perform(post("/banking/cards/me")
                        .with(user(EMAIL).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void findMyCard_DeveRetornar404_QuandoCartaoNaoExiste() throws Exception {
        when(creditCardService.findMyCard(EMAIL, "1234"))
                .thenThrow(new CreditCardNotFoundException(10L));

        AccountPasswordRequest body = new AccountPasswordRequest("1234");

        mockMvc.perform(post("/banking/cards/me")
                        .with(user(EMAIL).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void findMyLimit_DeveRetornar200ComLimite() throws Exception {
        CreditLimitResponse limit = new CreditLimitResponse(
                new BigDecimal("5000.00"),
                new BigDecimal("4500.00"),
                new BigDecimal("500.00")
        );
        when(creditCardService.findMyLimit(EMAIL)).thenReturn(limit);

        mockMvc.perform(get("/banking/cards/me/limit").with(user(EMAIL).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditLimit").value(5000.00))
                .andExpect(jsonPath("$.availableLimit").value(4500.00))
                .andExpect(jsonPath("$.usedLimit").value(500.00));

        verify(creditCardService).findMyLimit(EMAIL);
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void findMyLimit_DeveRetornar404_QuandoCartaoNaoExiste() throws Exception {
        when(creditCardService.findMyLimit(EMAIL))
                .thenThrow(new CreditCardNotFoundException(10L));

        mockMvc.perform(get("/banking/cards/me/limit").with(user(EMAIL).roles("CUSTOMER")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void findMyPurchases_DeveRetornar200ComLista() throws Exception {
        List<CardPurchaseResponse> purchases = List.of(
                new CardPurchaseResponse(1L, 100L, new BigDecimal("250.00"),
                        "Compra 1", "REF-1", Instant.now()),
                new CardPurchaseResponse(2L, 100L, new BigDecimal("75.50"),
                        "Compra 2", "REF-2", Instant.now())
        );
        when(creditCardService.findMyPurchases(EMAIL)).thenReturn(purchases);

        mockMvc.perform(get("/banking/cards/me/purchases").with(user(EMAIL).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].description").value("Compra 1"))
                .andExpect(jsonPath("$[0].amount").value(250.00))
                .andExpect(jsonPath("$[1].amount").value(75.50));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void findMyPurchases_DeveRetornar200ComListaVazia() throws Exception {
        when(creditCardService.findMyPurchases(EMAIL)).thenReturn(List.of());

        mockMvc.perform(get("/banking/cards/me/purchases").with(user(EMAIL).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
