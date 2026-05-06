package com.grupo.projeto.banking.repository;

import com.grupo.projeto.banking.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    Optional<Card> findByCardNumber(String cardNumber);
    
    Optional<Card> findByAccountId(Long accountId);
}