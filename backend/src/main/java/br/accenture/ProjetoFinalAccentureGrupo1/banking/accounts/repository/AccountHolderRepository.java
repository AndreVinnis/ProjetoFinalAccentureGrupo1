package br.accenture.ProjetoFinalAccentureGrupo1.banking.accounts.repository;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.accounts.domain.AccountHolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountHolderRepository extends JpaRepository<AccountHolder, Long> {
}
