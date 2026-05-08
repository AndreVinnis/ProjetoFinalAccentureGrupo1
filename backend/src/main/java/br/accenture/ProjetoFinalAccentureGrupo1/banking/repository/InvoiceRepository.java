package br.accenture.ProjetoFinalAccentureGrupo1.banking.repository;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Invoice;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByCardIdAndStatus(Long cardId, InvoiceStatus status);

    List<Invoice> findByCardIdOrderByReferenceMonthDesc(Long cardId);
}
