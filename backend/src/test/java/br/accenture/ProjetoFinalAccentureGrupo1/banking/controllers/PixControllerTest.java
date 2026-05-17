package br.accenture.ProjetoFinalAccentureGrupo1.banking.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.security.JwtAuthenticationFilter;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.PaymentRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.PayPixRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.PixRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.PaymentRequestStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.AccountNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.InsufficientBalanceException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.PaymentRequestNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.PaymentRequestNotPayableException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.WrongPasswordException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.PixService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PixController.class)
@AutoConfigureMockMvc(addFilters = false)
class PixControllerTest {

    private static final String PAYER = "ana@email.com";
    private static final String RECIPIENT = "bruno@email.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PixService pixService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private PaymentRequest pendingRequest;

    @BeforeEach
    void setUp() {
        Account recipient = Account.builder().id(2L).userId(20L).accountNumber("00002-0").build();
        pendingRequest = PaymentRequest.builder()
                .id(99L)
                .code("ABC-123")
                .recipient(recipient)
                .amount(new BigDecimal("75.50"))
                .description("Pagamento de teste")
                .reference("REF-99")
                .status(PaymentRequestStatus.PENDING)
                .createdAt(Instant.parse("2026-05-01T10:00:00Z"))
                .expiresAt(Instant.parse("2026-05-02T10:00:00Z"))
                .build();
    }

    @Test
    @WithMockUser(username = PAYER, roles = "CUSTOMER")
    void getByCode_DeveRetornar200ComPaymentRequest() throws Exception {
        when(pixService.getByCode("ABC-123")).thenReturn(pendingRequest);

        mockMvc.perform(get("/banking/pix/ABC-123").with(user(PAYER).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ABC-123"))
                .andExpect(jsonPath("$.amount").value(75.50))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(pixService).getByCode("ABC-123");
    }

    @Test
    @WithMockUser(username = PAYER, roles = "CUSTOMER")
    void getByCode_DeveRetornar404_QuandoCodigoNaoExiste() throws Exception {
        when(pixService.getByCode("INVALID"))
                .thenThrow(new PaymentRequestNotFoundException("INVALID"));

        mockMvc.perform(get("/banking/pix/INVALID").with(user(PAYER).roles("CUSTOMER")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = PAYER, roles = "CUSTOMER")
    void transfer_DeveRetornar200_QuandoDadosValidos() throws Exception {
        doNothing().when(pixService).passPix(
                eq(PAYER), eq("1234"), eq(RECIPIENT),
                any(BigDecimal.class), any());

        PixRequest body = new PixRequest(RECIPIENT, new BigDecimal("50.00"), "Almoço", "1234");

        mockMvc.perform(post("/banking/pix/transfer")
                        .with(user(PAYER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(pixService).passPix(PAYER, "1234", RECIPIENT, new BigDecimal("50.00"), "Almoço");
    }

    @Test
    @WithMockUser(username = PAYER, roles = "CUSTOMER")
    void transfer_DeveRetornar400_QuandoDestinatarioVazio() throws Exception {
        PixRequest body = new PixRequest("", new BigDecimal("50.00"), "x", "1234");

        mockMvc.perform(post("/banking/pix/transfer")
                        .with(user(PAYER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.recipientEmail").exists());
    }

    @Test
    @WithMockUser(username = PAYER, roles = "CUSTOMER")
    void transfer_DeveRetornar400_QuandoSenhaAusente() throws Exception {
        PixRequest body = new PixRequest(RECIPIENT, new BigDecimal("50.00"), "x", "");

        mockMvc.perform(post("/banking/pix/transfer")
                        .with(user(PAYER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    @WithMockUser(username = PAYER, roles = "CUSTOMER")
    void transfer_DeveRetornar400_QuandoValorNulo() throws Exception {
        String body = "{\"recipientEmail\":\"" + RECIPIENT + "\",\"password\":\"1234\"}";

        mockMvc.perform(post("/banking/pix/transfer")
                        .with(user(PAYER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.amount").exists());
    }

    @Test
    @WithMockUser(username = PAYER, roles = "CUSTOMER")
    void transfer_DeveRetornar400_QuandoSenhaIncorreta() throws Exception {
        // WrongPasswordException é mapeada para 400 no BankingExceptionHandler
        doThrow(new WrongPasswordException()).when(pixService).passPix(
                eq(PAYER), eq("wrong"), eq(RECIPIENT),
                any(BigDecimal.class), any());

        PixRequest body = new PixRequest(RECIPIENT, new BigDecimal("50.00"), "x", "wrong");

        mockMvc.perform(post("/banking/pix/transfer")
                        .with(user(PAYER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = PAYER, roles = "CUSTOMER")
    void transfer_DeveRetornar422_QuandoSaldoInsuficiente() throws Exception {
        doThrow(new InsufficientBalanceException(BigDecimal.TEN, new BigDecimal("50.00")))
                .when(pixService).passPix(eq(PAYER), eq("1234"), eq(RECIPIENT),
                        any(BigDecimal.class), any());

        PixRequest body = new PixRequest(RECIPIENT, new BigDecimal("50.00"), "x", "1234");

        mockMvc.perform(post("/banking/pix/transfer")
                        .with(user(PAYER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(username = PAYER, roles = "CUSTOMER")
    void transfer_DeveRetornar404_QuandoContaInexistente() throws Exception {
        doThrow(new AccountNotFoundException(20L))
                .when(pixService).passPix(eq(PAYER), eq("1234"), eq(RECIPIENT),
                        any(BigDecimal.class), any());

        PixRequest body = new PixRequest(RECIPIENT, new BigDecimal("50.00"), "x", "1234");

        mockMvc.perform(post("/banking/pix/transfer")
                        .with(user(PAYER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = PAYER, roles = "CUSTOMER")
    void pay_DeveRetornar200ComCobrancaPaga() throws Exception {
        PaymentRequest paid = PaymentRequest.builder()
                .id(99L).code("ABC-123")
                .recipient(pendingRequest.getRecipient())
                .amount(pendingRequest.getAmount())
                .description("Pagamento de teste")
                .reference("REF-99")
                .status(PaymentRequestStatus.PAID)
                .createdAt(pendingRequest.getCreatedAt())
                .expiresAt(pendingRequest.getExpiresAt())
                .paidAt(Instant.parse("2026-05-01T11:00:00Z"))
                .build();
        when(pixService.payByCode("ABC-123", PAYER, "1234")).thenReturn(paid);

        PayPixRequest body = new PayPixRequest("ABC-123", "1234");

        mockMvc.perform(post("/banking/pix/pay")
                        .with(user(PAYER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ABC-123"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt").exists());

        verify(pixService).payByCode("ABC-123", PAYER, "1234");
    }

    @Test
    @WithMockUser(username = PAYER, roles = "CUSTOMER")
    void pay_DeveRetornar400_QuandoCodigoAusente() throws Exception {
        PayPixRequest body = new PayPixRequest("", "1234");

        mockMvc.perform(post("/banking/pix/pay")
                        .with(user(PAYER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.code").exists());
    }

    @Test
    @WithMockUser(username = PAYER, roles = "CUSTOMER")
    void pay_DeveRetornar400_QuandoSenhaAusente() throws Exception {
        PayPixRequest body = new PayPixRequest("ABC-123", "");

        mockMvc.perform(post("/banking/pix/pay")
                        .with(user(PAYER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    @WithMockUser(username = PAYER, roles = "CUSTOMER")
    void pay_DeveRetornar404_QuandoCodigoNaoExiste() throws Exception {
        when(pixService.payByCode("INVALID", PAYER, "1234"))
                .thenThrow(new PaymentRequestNotFoundException("INVALID"));

        PayPixRequest body = new PayPixRequest("INVALID", "1234");

        mockMvc.perform(post("/banking/pix/pay")
                        .with(user(PAYER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = PAYER, roles = "CUSTOMER")
    void pay_DeveRetornar422_QuandoCobrancaNaoPagavel() throws Exception {
        when(pixService.payByCode("ABC-123", PAYER, "1234"))
                .thenThrow(new PaymentRequestNotPayableException("ABC-123", "EXPIRED"));

        PayPixRequest body = new PayPixRequest("ABC-123", "1234");

        mockMvc.perform(post("/banking/pix/pay")
                        .with(user(PAYER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }
}
