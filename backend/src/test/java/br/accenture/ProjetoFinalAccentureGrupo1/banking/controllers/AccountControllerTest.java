package br.accenture.ProjetoFinalAccentureGrupo1.banking.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.security.JwtAuthenticationFilter;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.AccountResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.DepositRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.TransactionResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountType;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.TransactionType;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.AccountBlockedException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.AccountNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.AccountService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    private static final String EMAIL = "ana@email.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private AccountResponse accountResponse;

    @BeforeEach
    void setUp() {
        accountResponse = new AccountResponse(
                1L,
                10L,
                "00001-0",
                new BigDecimal("100.00"),
                AccountType.CUSTOMER,
                AccountStatus.ACTIVE,
                Instant.parse("2026-01-01T10:00:00Z")
        );
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void findMyAccount_DeveRetornar200ComConta() throws Exception {
        when(accountService.findMyAccount(EMAIL)).thenReturn(accountResponse);

        mockMvc.perform(get("/banking/accounts/me").with(user(EMAIL).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountNumber").value("00001-0"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.balance").value(100.00));

        verify(accountService).findMyAccount(EMAIL);
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void findMyAccount_DeveRetornar404_QuandoContaNaoExiste() throws Exception {
        when(accountService.findMyAccount(EMAIL)).thenThrow(new AccountNotFoundException(10L));

        mockMvc.perform(get("/banking/accounts/me").with(user(EMAIL).roles("CUSTOMER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Conta")));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void findMyBalance_DeveRetornar200ComSaldo() throws Exception {
        when(accountService.getBalanceByEmail(EMAIL)).thenReturn(new BigDecimal("250.55"));

        mockMvc.perform(get("/banking/accounts/me/balance").with(user(EMAIL).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(250.55));

        verify(accountService).getBalanceByEmail(EMAIL);
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void deposit_DeveRetornar200ComContaAtualizada() throws Exception {
        AccountResponse afterDeposit = new AccountResponse(
                1L, 10L, "00001-0",
                new BigDecimal("150.00"),
                AccountType.CUSTOMER, AccountStatus.ACTIVE, Instant.now()
        );
        when(accountService.deposit(eq(EMAIL), any(BigDecimal.class), any()))
                .thenReturn(afterDeposit);

        DepositRequest body = new DepositRequest(new BigDecimal("50.00"), "Depósito via app");

        mockMvc.perform(post("/banking/accounts/me/deposit")
                        .with(user(EMAIL).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));

        verify(accountService).deposit(EMAIL, new BigDecimal("50.00"), "Depósito via app");
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void deposit_DeveRetornar400_QuandoValorAusente() throws Exception {
        String body = "{\"description\":\"x\"}";

        mockMvc.perform(post("/banking/accounts/me/deposit")
                        .with(user(EMAIL).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.amount").exists());
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void deposit_DeveRetornar400_QuandoValorMenorQueMinimo() throws Exception {
        DepositRequest body = new DepositRequest(new BigDecimal("0.00"), "x");

        mockMvc.perform(post("/banking/accounts/me/deposit")
                        .with(user(EMAIL).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.amount").exists());
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void deposit_DeveRetornar422_QuandoContaBloqueada() throws Exception {
        when(accountService.deposit(eq(EMAIL), any(BigDecimal.class), any()))
                .thenThrow(new AccountBlockedException());

        DepositRequest body = new DepositRequest(new BigDecimal("10.00"), "x");

        mockMvc.perform(post("/banking/accounts/me/deposit")
                        .with(user(EMAIL).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void findMyTransactions_DeveRetornar200ComLista() throws Exception {
        List<TransactionResponse> txs = List.of(
                new TransactionResponse(1L, TransactionType.DEPOSIT, new BigDecimal("10.00"),
                        new BigDecimal("110.00"), "REF-1", "Depósito", Instant.now()),
                new TransactionResponse(2L, TransactionType.PAYMENT, new BigDecimal("5.00"),
                        new BigDecimal("105.00"), "REF-2", "Compra", Instant.now())
        );
        when(accountService.listMyTransactions(EMAIL)).thenReturn(txs);

        mockMvc.perform(get("/banking/accounts/me/transactions").with(user(EMAIL).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[1].type").value("PAYMENT"));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void findMyTransactions_DeveRetornar200ComListaVazia_QuandoNaoHaTransacoes() throws Exception {
        when(accountService.listMyTransactions(EMAIL)).thenReturn(List.of());

        mockMvc.perform(get("/banking/accounts/me/transactions").with(user(EMAIL).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
