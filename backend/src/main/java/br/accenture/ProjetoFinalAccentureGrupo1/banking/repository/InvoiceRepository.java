package br.accenture.ProjetoFinalAccentureGrupo1.banking.repository;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Invoice;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByCardIdAndStatus(Long cardId, InvoiceStatus status);

    Optional<Invoice> findByCardIdAndReferenceMonth(Long cardId, YearMonth referenceMonth);

    List<Invoice> findByCardIdOrderByReferenceMonthDesc(Long cardId);

    List<Invoice> findByStatusAndDueDateLessThanEqual(InvoiceStatus invoiceStatus, LocalDate today);

    List<Invoice> findByStatusAndClosingDateLessThanEqual(InvoiceStatus invoiceStatus, LocalDate today);
}
