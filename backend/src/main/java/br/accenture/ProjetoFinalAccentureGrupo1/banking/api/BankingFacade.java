package br.accenture.ProjetoFinalAccentureGrupo1.banking.api;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CardValidationResponse;

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

    CardValidationResponse verifyCard(String cardNumber, String cvv, int expirationMonth, int expirationYear);

    void chargeCard(Long cardId, BigDecimal amount, String cvv, String description, String reference);

    void issueRefund(Long toUserId, BigDecimal amount, String reference, String description);

    void applyCashback(Long toUserId, BigDecimal amount, String reference, String description);

    /**
     * Cria uma cobrança PIX em nome da empresa.
     * amount valor a ser cobrado
     * description descrição amigável (ex: "Pagamento do pedido #42")
     * reference referência externa (ex: "ORDER-42")
     * código UUID que o pagador usa pra confirmar
     */
    String createPaymentRequest(BigDecimal amount, String description, String reference);

    /**
     * Cancela uma cobrança PIX em PENDING (ex: pedido cancelado antes de pagar).
     * Sem efeito financeiro — apenas muda o status.
     */
    void cancelPaymentRequest(String code);
}
