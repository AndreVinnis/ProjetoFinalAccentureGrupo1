package br.accenture.ProjetoFinalAccentureGrupo1.banking.domain;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.domain.User;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.CreditCardStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "credit_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", unique = true)
    private Account account;

    @Column(name = "holder_name", nullable = false, length = 100)
    private String holderName;

    @Column(name = "number_hash", nullable = false, unique = true, length = 64)
    private String numberHash;

    @Column(name = "last_four_digits", nullable = false, length = 4)
    private String lastFourDigits;

    @Column(name = "cvv_hash", nullable = false, length = 64)
    private String cvvHash;

    @Column(name = "expiration_month", nullable = false)
    private int expirationMonth;

    @Column(name = "expiration_year", nullable = false)
    private int expirationYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CreditCardStatus status;

    @Column(name = "credit_limit", nullable = false, precision = 14, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "available_limit", nullable = false, precision = 14, scale = 2)
    private BigDecimal availableLimit;

    @Column(name = "invoice_balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal invoiceBalance;

    @Column(name = "closing_day")
    private Integer closingDay;

    @Column(name = "due_day")
    private Integer dueDay;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.closingDay == null || this.closingDay == 0) {
            this.closingDay = 25;
        }
        if (this.dueDay == null || this.dueDay == 0) {
            this.dueDay = 10;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
