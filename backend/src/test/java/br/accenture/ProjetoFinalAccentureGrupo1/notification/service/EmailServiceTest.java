package br.accenture.ProjetoFinalAccentureGrupo1.notification.service;

import br.accenture.ProjetoFinalAccentureGrupo1.notification.domain.EmailLog;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.domain.EmailMessage;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.enums.EmailStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.repository.EmailLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// André Vinícius Barros Macambira
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailLogRepository emailLogRepository;

    @InjectMocks
    private EmailService emailService;

    private EmailMessage message;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@test.local");

        message = new EmailMessage(
                "ana@email.com",
                "Pedido #42 confirmado",
                "Olá Ana, seu pedido foi pago.",
                "ORDER_PAID"
        );

        EmailLog savedLog = EmailLog.builder()
                .id(1L)
                .recipient(message.recipient())
                .subject(message.subject())
                .type(message.type())
                .status(EmailStatus.PENDING)
                .build();
        when(emailLogRepository.save(any(EmailLog.class))).thenReturn(savedLog);
    }

    @Test
    void send_DeveEnviarEMarcarComoSent_QuandoEnvioOk() {
        emailService.send(message);

        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(emailLogRepository, times(2)).save(any(EmailLog.class));

        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository, times(2)).save(captor.capture());
        EmailLog finalLog = captor.getAllValues().get(1);
        assertEquals(EmailStatus.SENT, finalLog.getStatus());
    }

    @Test
    void send_DeveMarcarComoFailedEReLancarExcecao_QuandoEnvioFalha() {
        doThrow(new RuntimeException("SMTP timeout"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> emailService.send(message));
        assertEquals("SMTP timeout", ex.getMessage());

        // Ainda assim salvou 2 vezes (PENDING + FAILED)
        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository, times(2)).save(captor.capture());
        EmailLog finalLog = captor.getAllValues().get(1);
        assertEquals(EmailStatus.FAILED, finalLog.getStatus());
        assertEquals("SMTP timeout", finalLog.getErrorMessage());
    }
}
