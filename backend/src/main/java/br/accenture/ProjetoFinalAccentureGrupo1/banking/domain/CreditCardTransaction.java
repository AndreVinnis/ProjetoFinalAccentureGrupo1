package br.accenture.ProjetoFinalAccentureGrupo1.banking.domain;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.enums.CreditCardTransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "credit_card_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditCardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_card_id", nullable = false)
    private CreditCard creditCard;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "merchant_name", nullable = false, length = 120)
    private String merchantName;

    @Column(nullable = false, length = 255)
    private String description;

    @Column
    private Integer installments;

    @Column(name = "installment_amount", precision = 14, scale = 2)
    private BigDecimal installmentAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CreditCardTransactionStatus status;

    @Column(name = "decline_reason", length = 255)
    private String declineReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.installments == null) {
            this.installments = 1;
        }
        if (this.installmentAmount == null) {
            this.installmentAmount = this.amount;
        }
        this.createdAt = Instant.now();
    }
}
