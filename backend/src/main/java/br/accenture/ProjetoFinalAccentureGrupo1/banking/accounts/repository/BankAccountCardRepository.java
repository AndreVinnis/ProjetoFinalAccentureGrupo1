package br.accenture.ProjetoFinalAccentureGrupo1.banking.accounts.repository;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.accounts.domain.BankAccountCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankAccountCardRepository extends JpaRepository<BankAccountCard, Long> {

    Optional<BankAccountCard> findByCardNumber(String cardNumber);

    Optional<BankAccountCard> findByAccount_Id(Long accountId);
}
