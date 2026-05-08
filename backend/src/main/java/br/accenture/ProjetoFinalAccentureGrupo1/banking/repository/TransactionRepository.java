package br.accenture.ProjetoFinalAccentureGrupo1.banking.repository;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);
}
