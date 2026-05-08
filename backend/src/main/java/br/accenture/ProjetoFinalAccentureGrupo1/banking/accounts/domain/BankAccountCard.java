package br.accenture.ProjetoFinalAccentureGrupo1.banking.accounts.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Cartão vinculado a uma conta corrente (modelo do módulo accounts).
 * Distinto de {@link br.accenture.ProjetoFinalAccentureGrupo1.banking.domain.CreditCard} (cartão de crédito do e-commerce).
 */
@Entity
@Table(name = "bank_account_cards")
@Getter
@Setter
public class BankAccountCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(unique = true)
    private String cardNumber;

    private BigDecimal limitTotal;
    private BigDecimal limitAvailable;
    private int closingDay;
    private int dueDay;

    @Enumerated(EnumType.STRING)
    private BankCardStatus status = BankCardStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
