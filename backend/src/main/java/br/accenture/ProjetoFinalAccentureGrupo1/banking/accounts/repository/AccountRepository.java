package br.accenture.ProjetoFinalAccentureGrupo1.banking.accounts.repository;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.accounts.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByHolder_Id(Long holderId);

    Optional<Account> findByHolder_UserId(Long userId);
}
