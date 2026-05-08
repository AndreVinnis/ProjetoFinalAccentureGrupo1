package br.accenture.ProjetoFinalAccentureGrupo1.notification.worker;

import br.accenture.ProjetoFinalAccentureGrupo1.notification.domain.EmailMessage;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailWorkerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailWorker worker;

    @Test
    void handle_DeveDelegarParaEmailService() {
        EmailMessage message = new EmailMessage(
                "a@b.com", "Subject", "Body", "ORDER_PAID"
        );

        worker.handle(message);

        verify(emailService).send(message);
    }
}
