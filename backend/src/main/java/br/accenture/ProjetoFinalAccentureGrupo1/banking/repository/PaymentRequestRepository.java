package br.accenture.ProjetoFinalAccentureGrupo1.banking.repository;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.PaymentRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.PaymentRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
 // André Vinícius Barros Macambira
public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {

    Optional<PaymentRequest> findByCode(String code);

    Optional<PaymentRequest> findByReference(String reference);

    List<PaymentRequest> findByStatusAndExpiresAtBefore(PaymentRequestStatus status, Instant moment);
}
