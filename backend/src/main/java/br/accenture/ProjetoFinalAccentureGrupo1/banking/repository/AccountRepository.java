package br.accenture.ProjetoFinalAccentureGrupo1.banking.repository;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Account;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
/* Autores:
 * Antônio Hortêncio Batista Rocha de Queiroga
 * André Vinícius Barros Macambira
 */
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUserId(Long userId);

    Optional<Account> findByAccountNumber(String accountNumber);

    // Usado pelo CompanyAccountInitializer para encontrar a conta da empresa
    Optional<Account> findFirstByAccountType(AccountType accountType);
}
