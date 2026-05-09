package br.accenture.ProjetoFinalAccentureGrupo1.banking.api;

import java.math.BigDecimal;

/**
 * Fachada para outros módulos (ex.: e-commerce). {@code userId} é sempre o {@code users.id} (mesmo do JWT/cadastro).
 */
public interface BankingFacade {

    BigDecimal getBalance(Long userId);

    /** Débito do saldo em conta corrente do cliente. */
    void chargeCard(Long userId, BigDecimal amount, String description, String reference);

    void issueRefund(Long toUserId, BigDecimal amount, String description);
}
