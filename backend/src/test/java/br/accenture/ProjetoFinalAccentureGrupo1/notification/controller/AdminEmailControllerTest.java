package br.accenture.ProjetoFinalAccentureGrupo1.notification.controller;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.security.JwtAuthenticationFilter;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.domain.EmailLog;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.enums.EmailStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.repository.EmailLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminEmailController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminEmailControllerTest {

    private static final String ADMIN = "admin@email.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailLogRepository emailLogRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private EmailLog sentLog;
    private EmailLog failedLog;

    @BeforeEach
    void setUp() {
        sentLog = EmailLog.builder()
                .id(1L)
                .recipient("ana@email.com")
                .subject("Confirmação de pedido")
                .type("ORDER_PAID")
                .status(EmailStatus.SENT)
                .createdAt(Instant.parse("2026-05-10T10:00:00Z"))
                .sentAt(Instant.parse("2026-05-10T10:00:05Z"))
                .build();

        failedLog = EmailLog.builder()
                .id(2L)
                .recipient("bruno@email.com")
                .subject("Fatura disponível")
                .type("INVOICE_OVERDUE")
                .status(EmailStatus.FAILED)
                .errorMessage("SMTP timeout")
                .createdAt(Instant.parse("2026-05-11T08:00:00Z"))
                .build();
    }

    @Test
    void list_DeveRetornar200ComPaginaSemFiltros() throws Exception {
        Page<EmailLog> page = new PageImpl<>(
                List.of(sentLog, failedLog),
                PageRequest.of(0, 20),
                2
        );
        when(emailLogRepository.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/admin/notifications/emails")
                        .with(user(ADMIN).roles("ECOMMERCE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].recipient").value("ana@email.com"))
                .andExpect(jsonPath("$.content[0].status").value("SENT"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].status").value("FAILED"))
                .andExpect(jsonPath("$.content[1].errorMessage").value("SMTP timeout"))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(emailLogRepository).findAll(any(Pageable.class));
        verify(emailLogRepository, never()).findByStatus(any(), any());
        verify(emailLogRepository, never()).findByType(any(), any());
    }

    @Test
    void list_DeveRetornar200ComPaginaVazia() throws Exception {
        Page<EmailLog> empty = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(emailLogRepository.findAll(any(Pageable.class))).thenReturn(empty);

        mockMvc.perform(get("/admin/notifications/emails")
                        .with(user(ADMIN).roles("BANKING_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void list_DeveFiltrarPorStatus_QuandoStatusInformado() throws Exception {
        Page<EmailLog> page = new PageImpl<>(List.of(failedLog), PageRequest.of(0, 20), 1);
        when(emailLogRepository.findByStatus(eq(EmailStatus.FAILED), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/admin/notifications/emails")
                        .param("status", "FAILED")
                        .with(user(ADMIN).roles("BANKING_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("FAILED"));

        verify(emailLogRepository).findByStatus(eq(EmailStatus.FAILED), any(Pageable.class));
        verify(emailLogRepository, never()).findAll(any(Pageable.class));
        verify(emailLogRepository, never()).findByType(any(), any());
    }

    @Test
    void list_DeveRetornar400_QuandoStatusInvalido() throws Exception {
        mockMvc.perform(get("/admin/notifications/emails")
                        .param("status", "INVALIDO")
                        .with(user(ADMIN).roles("BANKING_ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_DeveFiltrarPorType_QuandoTypeInformado() throws Exception {
        Page<EmailLog> page = new PageImpl<>(List.of(sentLog), PageRequest.of(0, 20), 1);
        when(emailLogRepository.findByType(eq("ORDER_PAID"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/admin/notifications/emails")
                        .param("type", "ORDER_PAID")
                        .with(user(ADMIN).roles("ECOMMERCE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("ORDER_PAID"));

        verify(emailLogRepository).findByType(eq("ORDER_PAID"), any(Pageable.class));
        verify(emailLogRepository, never()).findAll(any(Pageable.class));
        verify(emailLogRepository, never()).findByStatus(any(), any());
    }

    @Test
    void list_DevePriorizarStatus_QuandoStatusETypeInformados() throws Exception {
        // O controller checa status primeiro; type é ignorado quando ambos vêm juntos.
        Page<EmailLog> page = new PageImpl<>(List.of(failedLog), PageRequest.of(0, 20), 1);
        when(emailLogRepository.findByStatus(eq(EmailStatus.FAILED), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/admin/notifications/emails")
                        .param("status", "FAILED")
                        .param("type", "ORDER_PAID")
                        .with(user(ADMIN).roles("ECOMMERCE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("FAILED"));

        verify(emailLogRepository).findByStatus(eq(EmailStatus.FAILED), any(Pageable.class));
        verify(emailLogRepository, never()).findByType(any(), any());
    }

    @Test
    void list_DeveRespeitarParametrosDePaginacao() throws Exception {
        Page<EmailLog> page = new PageImpl<>(List.of(sentLog), PageRequest.of(1, 5), 6);
        when(emailLogRepository.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/admin/notifications/emails")
                        .param("page", "1")
                        .param("size", "5")
                        .with(user(ADMIN).roles("BANKING_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.totalElements").value(6));
    }

    @Test
    void getById_DeveRetornar200ComEmailLog_QuandoExistir() throws Exception {
        when(emailLogRepository.findById(1L)).thenReturn(Optional.of(sentLog));

        mockMvc.perform(get("/admin/notifications/emails/1")
                        .with(user(ADMIN).roles("ECOMMERCE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.recipient").value("ana@email.com"))
                .andExpect(jsonPath("$.subject").value("Confirmação de pedido"))
                .andExpect(jsonPath("$.type").value("ORDER_PAID"))
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.sentAt").exists());

        verify(emailLogRepository).findById(1L);
    }

    @Test
    void getById_DeveRetornar404_QuandoNaoExistir() throws Exception {
        when(emailLogRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/notifications/emails/99")
                        .with(user(ADMIN).roles("BANKING_ADMIN")))
                .andExpect(status().isNotFound());

        verify(emailLogRepository).findById(99L);
    }

    @Test
    void getById_DeveRetornar400_QuandoIdNaoNumerico() throws Exception {
        mockMvc.perform(get("/admin/notifications/emails/abc")
                        .with(user(ADMIN).roles("BANKING_ADMIN")))
                .andExpect(status().isBadRequest());
    }
}
