package br.accenture.ProjetoFinalAccentureGrupo1.notification.service;

import br.accenture.ProjetoFinalAccentureGrupo1.notification.domain.EmailLog;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.domain.EmailMessage;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.enums.EmailStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// André Vinícius Barros Macambira
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;

    @Value("${spring.mail.username:noreply@accenture.local}")
    private String fromAddress;

    public void send(EmailMessage message) {
        EmailLog log = EmailLog.builder()
                .recipient(message.recipient())
                .subject(message.subject())
                .type(message.type())
                .status(EmailStatus.PENDING)
                .build();
        log = emailLogRepository.save(log);

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setTo(message.recipient());
            mail.setSubject(message.subject());
            mail.setText(message.body());
            mailSender.send(mail);

            log.markSent();
            emailLogRepository.save(log);
        } catch (Exception ex) {
            log.markFailed(ex.getMessage());
            emailLogRepository.save(log);
            throw ex;  // re-throw pra o Rabbit fazer retry
        }
    }
}