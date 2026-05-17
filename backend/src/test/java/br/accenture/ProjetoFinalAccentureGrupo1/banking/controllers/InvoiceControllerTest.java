package br.accenture.ProjetoFinalAccentureGrupo1.banking.controllers;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.security.JwtAuthenticationFilter;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.InvoiceResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.PayInvoiceRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.InvoiceStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.InsufficientBalanceException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.InvoiceNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions.InvoiceNotPayableException;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.services.InvoiceService;
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
import java.time.LocalDate;
import java.time.YearMonth;
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

@WebMvcTest(controllers = InvoiceController.class)
@AutoConfigureMockMvc(addFilters = false)
class InvoiceControllerTest {

    private static final String EMAIL = "ana@email.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InvoiceService invoiceService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private InvoiceResponse openInvoice;

    @BeforeEach
    void setUp() {
        openInvoice = new InvoiceResponse(
                100L,
                YearMonth.of(2026, 5),
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 6, 10),
                new BigDecimal("250.00"),
                BigDecimal.ZERO,
                InvoiceStatus.OPEN,
                null,
                null
        );
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void findCurrentInvoice_DeveRetornar200ComFaturaAberta() throws Exception {
        when(invoiceService.getCurrentInvoice(EMAIL)).thenReturn(openInvoice);

        mockMvc.perform(get("/banking/invoices/current").with(user(EMAIL).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.totalAmount").value(250.00));

        verify(invoiceService).getCurrentInvoice(EMAIL);
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void listInvoices_DeveRetornar200ComLista() throws Exception {
        InvoiceResponse closed = new InvoiceResponse(
                99L, YearMonth.of(2026, 4),
                LocalDate.of(2026, 4, 25), LocalDate.of(2026, 5, 10),
                new BigDecimal("180.00"), new BigDecimal("180.00"),
                InvoiceStatus.PAID, Instant.now(), Instant.now()
        );
        when(invoiceService.listByCard(EMAIL)).thenReturn(List.of(openInvoice, closed));

        mockMvc.perform(get("/banking/invoices").with(user(EMAIL).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[1].status").value("PAID"));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void listInvoices_DeveRetornar200ComListaVazia() throws Exception {
        when(invoiceService.listByCard(EMAIL)).thenReturn(List.of());

        mockMvc.perform(get("/banking/invoices").with(user(EMAIL).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void payInvoice_DeveRetornar200ComFaturaPaga() throws Exception {
        InvoiceResponse paid = new InvoiceResponse(
                100L, YearMonth.of(2026, 5),
                LocalDate.of(2026, 5, 25), LocalDate.of(2026, 6, 10),
                new BigDecimal("250.00"), new BigDecimal("250.00"),
                InvoiceStatus.PAID, Instant.now(), Instant.now()
        );
        when(invoiceService.payInvoice(eq(100L), any(BigDecimal.class))).thenReturn(paid);

        PayInvoiceRequest body = new PayInvoiceRequest(new BigDecimal("250.00"));

        mockMvc.perform(post("/banking/invoices/100/pay")
                        .with(user(EMAIL).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAmount").value(250.00));

        verify(invoiceService).payInvoice(100L, new BigDecimal("250.00"));
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void payInvoice_DeveRetornar400_QuandoValorAusente() throws Exception {
        mockMvc.perform(post("/banking/invoices/100/pay")
                        .with(user(EMAIL).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.amount").exists());
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void payInvoice_DeveRetornar400_QuandoValorAbaixoDoMinimo() throws Exception {
        PayInvoiceRequest body = new PayInvoiceRequest(new BigDecimal("0.00"));

        mockMvc.perform(post("/banking/invoices/100/pay")
                        .with(user(EMAIL).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.amount").exists());
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void payInvoice_DeveRetornar404_QuandoFaturaNaoExiste() throws Exception {
        when(invoiceService.payInvoice(eq(999L), any(BigDecimal.class)))
                .thenThrow(new InvoiceNotFoundException(999L));

        PayInvoiceRequest body = new PayInvoiceRequest(new BigDecimal("10.00"));

        mockMvc.perform(post("/banking/invoices/999/pay")
                        .with(user(EMAIL).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void payInvoice_DeveRetornar422_QuandoFaturaNaoPagavel() throws Exception {
        when(invoiceService.payInvoice(eq(100L), any(BigDecimal.class)))
                .thenThrow(new InvoiceNotPayableException(100L, InvoiceStatus.OPEN));

        PayInvoiceRequest body = new PayInvoiceRequest(new BigDecimal("10.00"));

        mockMvc.perform(post("/banking/invoices/100/pay")
                        .with(user(EMAIL).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "CUSTOMER")
    void payInvoice_DeveRetornar422_QuandoSaldoInsuficiente() throws Exception {
        when(invoiceService.payInvoice(eq(100L), any(BigDecimal.class)))
                .thenThrow(new InsufficientBalanceException(new BigDecimal("5.00"), new BigDecimal("250.00")));

        PayInvoiceRequest body = new PayInvoiceRequest(new BigDecimal("250.00"));

        mockMvc.perform(post("/banking/invoices/100/pay")
                        .with(user(EMAIL).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }
}
