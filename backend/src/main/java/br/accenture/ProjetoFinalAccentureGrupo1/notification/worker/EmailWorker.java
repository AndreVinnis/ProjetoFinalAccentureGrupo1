package br.accenture.ProjetoFinalAccentureGrupo1.notification.worker;

import br.accenture.ProjetoFinalAccentureGrupo1.notification.config.RabbitConfig;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.domain.EmailMessage;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/*
 * Consome mensagens da fila email.queue e delega o envio ao EmailService.
 * André Vinícius Barros Macambira
 */

@Component
@RequiredArgsConstructor
public class EmailWorker {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitConfig.EMAIL_QUEUE)
    public void handle(EmailMessage message) {
        emailService.send(message);
    }
}
