package br.accenture.ProjetoFinalAccentureGrupo1.banking.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.security.JwtAuthenticationFilter;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Transaction;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.AccountResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.DepositRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountType;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.TransactionType;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.AccountNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.InvoiceNotCloseableException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.InvoiceNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.AccountService;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.BillingScheduler;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.CreditCardService;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.InvoiceService;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.TransactionService;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.InvoiceStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAccountControllerTest {

    private static final String ADMIN = "admin@email.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean private AccountService accountService;
    @MockitoBean private CreditCardService creditCardService;
    @MockitoBean private TransactionService transactionService;
    @MockitoBean private InvoiceService invoiceService;
    @MockitoBean private BillingScheduler billingScheduler;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    private AccountResponse accountResponse;
    private Account account;

    @BeforeEach
    void setUp() {
        accountResponse = new AccountResponse(
                1L, 10L, "00001-0",
                new BigDecimal("500.00"),
                AccountType.CUSTOMER, AccountStatus.ACTIVE,
                Instant.parse("2026-01-01T10:00:00Z")
        );
        account = Account.builder()
                .id(1L).userId(10L).accountNumber("00001-0")
                .balance(new BigDecimal("500.00"))
                .accountType(AccountType.CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void getAllAccounts_DeveRetornar200ComLista() throws Exception {
        when(accountService.findAllAccounts()).thenReturn(List.of(accountResponse));

        mockMvc.perform(get("/banking/admin").with(user(ADMIN).roles("BANKING_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].accountNumber").value("00001-0"));
    }

    @Test
    void getAllTransactions_DeveRetornar200ComListaVazia() throws Exception {
        when(transactionService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/banking/admin/accounts/transactions")
                        .with(user(ADMIN).roles("BANKING_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAllTransactions_DeveRetornar200ComLista() throws Exception {
        Transaction tx = Transaction.builder()
                .id(1L)
                .type(TransactionType.DEPOSIT)
                .amount(new BigDecimal("10.00"))
                .balanceAfter(new BigDecimal("510.00"))
                .reference("REF-1")
                .description("Depósito")
                .createdAt(Instant.now())
                .build();
        when(transactionService.findAll()).thenReturn(List.of(tx));

        mockMvc.perform(get("/banking/admin/accounts/transactions")
                        .with(user(ADMIN).roles("BANKING_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"));
    }

    @Test
    void deposit_DeveRetornar200ComContaAtualizada() throws Exception {
        AccountResponse updated = new AccountResponse(
                1L, 10L, "00001-0",
                new BigDecimal("600.00"),
                AccountType.CUSTOMER, AccountStatus.ACTIVE, Instant.now()
        );
        when(accountService.adminDeposit(eq(1L), any(BigDecimal.class), any()))
                .thenReturn(updated);

        DepositRequest body = new DepositRequest(new BigDecimal("100.00"), "Bônus");

        mockMvc.perform(post("/banking/admin/accounts/1/deposit")
                        .with(user(ADMIN).roles("BANKING_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(600.00));

        verify(accountService).adminDeposit(1L, new BigDecimal("100.00"), "Bônus");
    }

    @Test
    void deposit_DeveRetornar404_QuandoContaNaoExiste() throws Exception {
        when(accountService.adminDeposit(eq(99L), any(BigDecimal.class), any()))
                .thenThrow(new AccountNotFoundException(99L));

        DepositRequest body = new DepositRequest(new BigDecimal("10.00"), null);

        mockMvc.perform(post("/banking/admin/accounts/99/deposit")
                        .with(user(ADMIN).roles("BANKING_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void blockAccount_DeveBuscarContaEBloquearCartaoEConta() throws Exception {
        AccountResponse blocked = new AccountResponse(
                1L, 10L, "00001-0",
                new BigDecimal("500.00"),
                AccountType.CUSTOMER, AccountStatus.BLOCKED, Instant.now()
        );

        when(accountService.findByIdAdmin(1L)).thenReturn(account);
        doNothing().when(creditCardService).blockCardByAccount(account);
        when(accountService.blockAccount(1L)).thenReturn(blocked);

        mockMvc.perform(post("/banking/admin/accounts/1/block")
                        .with(user(ADMIN).roles("BANKING_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        verify(accountService).findByIdAdmin(1L);
        verify(creditCardService).blockCardByAccount(account);
        verify(accountService).blockAccount(1L);
    }

    @Test
    void blockAccount_DeveRetornar404_QuandoContaNaoExiste() throws Exception {
        when(accountService.findByIdAdmin(99L)).thenThrow(new AccountNotFoundException(99L));

        mockMvc.perform(post("/banking/admin/accounts/99/block")
                        .with(user(ADMIN).roles("BANKING_ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void unBlockAccount_DeveDesbloquearCartaoEConta() throws Exception {
        when(accountService.findByIdAdmin(1L)).thenReturn(account);
        doNothing().when(creditCardService).unblockCardByAccount(account);
        when(accountService.unBlockAccount(1L)).thenReturn(accountResponse);

        mockMvc.perform(post("/banking/admin/accounts/1/unblock")
                        .with(user(ADMIN).roles("BANKING_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(creditCardService).unblockCardByAccount(account);
        verify(accountService).unBlockAccount(1L);
    }

    @Test
    void closeInvoice_DeveRetornar200() throws Exception {
        when(invoiceService.closeInvoice(50L)).thenReturn(null);

        mockMvc.perform(post("/banking/admin/billing/invoices/50/close")
                        .with(user(ADMIN).roles("BANKING_ADMIN")))
                .andExpect(status().isOk());

        verify(invoiceService).closeInvoice(50L);
    }

    @Test
    void closeInvoice_DeveRetornar404_QuandoFaturaNaoExiste() throws Exception {
        when(invoiceService.closeInvoice(50L)).thenThrow(new InvoiceNotFoundException(50L));

        mockMvc.perform(post("/banking/admin/billing/invoices/50/close")
                        .with(user(ADMIN).roles("BANKING_ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void closeInvoice_DeveRetornar422_QuandoFaturaNaoFechavel() throws Exception {
        when(invoiceService.closeInvoice(50L))
                .thenThrow(new InvoiceNotCloseableException(50L, InvoiceStatus.PAID));

        mockMvc.perform(post("/banking/admin/billing/invoices/50/close")
                        .with(user(ADMIN).roles("BANKING_ADMIN")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void chargeInvoices_DeveRetornar200() throws Exception {
        when(invoiceService.chargeDueInvoices()).thenReturn(3);

        mockMvc.perform(post("/banking/admin/billing/charge-overdue")
                        .with(user(ADMIN).roles("BANKING_ADMIN")))
                .andExpect(status().isOk());

        verify(invoiceService, times(1)).chargeDueInvoices();
    }

    @Test
    void runDayBilling_DeveRetornar200EChamarScheduler() throws Exception {
        doNothing().when(billingScheduler).runDailyBilling();

        mockMvc.perform(post("/banking/admin/billing/run-day")
                        .with(user(ADMIN).roles("BANKING_ADMIN")))
                .andExpect(status().isOk());

        verify(billingScheduler).runDailyBilling();
    }

    @Test
    void runDayBilling_DevePropagarErroQuandoSchedulerFalha() throws Exception {
        doThrow(new RuntimeException("falha"))
                .when(billingScheduler).runDailyBilling();

        ServletException exception = assertThrows(ServletException.class, () -> {
            mockMvc.perform(post("/banking/admin/billing/run-day")
                    .with(user(ADMIN).roles("BANKING_ADMIN")));
        });

        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("falha", exception.getCause().getMessage());
    }

}
