package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.SavedCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedCardRepository extends JpaRepository<SavedCard, Long> {

    List<SavedCard> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);

    Optional<SavedCard> findByIdAndCustomer_Id(Long id, Long customerId);

    boolean existsByCustomer_IdAndBankingCardId(Long customerId, Long bankingCardId);
}
