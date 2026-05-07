package br.accenture.ProjetoFinalAccentureGrupo1.notification.repository;

import br.accenture.ProjetoFinalAccentureGrupo1.notification.domain.EmailLog;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.enums.EmailStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
// Autor: André Vinícius Barros Macambira
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    Page<EmailLog> findByStatus(EmailStatus status, Pageable pageable);

    Page<EmailLog> findByType(String type, Pageable pageable);

    Page<EmailLog> findByRecipient(String recipient, Pageable pageable);
}
