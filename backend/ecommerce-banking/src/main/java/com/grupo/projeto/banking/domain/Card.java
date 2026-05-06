package com.grupo.projeto.banking.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "account_id")
    private Account account; 

    @Column(unique = true)
    private String cardNumber;

    private BigDecimal limitTotal;
    private BigDecimal limitAvailable;
    private int closingDay;
    private int dueDay;

    @Enumerated(EnumType.STRING)
    private CardStatus status = CardStatus.ACTIVE;

    private Instant createdAt = Instant.now();

    // GETTERS E SETTERS MANUAIS 

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public BigDecimal getLimitTotal() { return limitTotal; }
    public void setLimitTotal(BigDecimal limitTotal) { this.limitTotal = limitTotal; }

    public BigDecimal getLimitAvailable() { return limitAvailable; }
    public void setLimitAvailable(BigDecimal limitAvailable) { this.limitAvailable = limitAvailable; }

    public int getClosingDay() { return closingDay; }
    public void setClosingDay(int closingDay) { this.closingDay = closingDay; }

    public int getDueDay() { return dueDay; }
    public void setDueDay(int dueDay) { this.dueDay = dueDay; }

    public CardStatus getStatus() { return status; }
    public void setStatus(CardStatus status) { this.status = status; }
}