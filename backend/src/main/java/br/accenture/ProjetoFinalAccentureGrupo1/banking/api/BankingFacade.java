package br.accenture.ProjetoFinalAccentureGrupo1.banking.api;

import java.math.BigDecimal;

/**
 * Fachada para outros módulos (ex.: e-commerce). {@code userId} é sempre o {@code users.id} (mesmo do JWT/cadastro).
 * Autores:
 * Antônio Hortêncio Batista Rocha de Queiroga
 * André Vinícius Barros Macambira
 */
public interface BankingFacade {

    BigDecimal getBalance(Long userId);

    AccountInfo getAccountInfo(Long userId);

    /** Débito do saldo em conta corrente do cliente. */
    void chargeCard(Long userId, BigDecimal amount, String description, String reference);

    void issueRefund(Long toUserId, BigDecimal amount, String description);

    /**
     * Cria uma cobrança PIX em nome da empresa.
     * @param amount valor a ser cobrado
     * @param description descrição amigável (ex: "Pagamento do pedido #42")
     * @param reference referência externa (ex: "ORDER-42")
     * @return código UUID que o pagador usa pra confirmar
     */
    String createPaymentRequest(BigDecimal amount, String description, String reference);
}
