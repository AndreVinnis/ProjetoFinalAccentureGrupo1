package com.grupo.projeto.banking.repository; // Onde este arquivo está

import com.grupo.projeto.banking.domain.Transaction; // Importando a classe do outro pacote
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}