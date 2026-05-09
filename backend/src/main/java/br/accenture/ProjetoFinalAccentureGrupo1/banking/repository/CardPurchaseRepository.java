package br.accenture.ProjetoFinalAccentureGrupo1.banking.repository;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CardPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardPurchaseRepository extends JpaRepository<CardPurchase, Long> {

    List<CardPurchase> findByCardIdOrderByPurchaseDateDesc(Long cardId);

    List<CardPurchase> findByInvoiceIdOrderByPurchaseDateDesc(Long invoiceId);
}
