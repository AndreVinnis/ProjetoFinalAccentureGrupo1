package com.grupo.projeto.banking.api;

import java.math.BigDecimal;

public interface BankingFacade {
    BigDecimal getBalance(Long userId);
    void chargeCard(Long userId, BigDecimal amount, String description, String reference);
    void issueRefund(Long toUserId, BigDecimal amount, String description);
}