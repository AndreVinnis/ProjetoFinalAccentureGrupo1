package com.grupo.projeto.banking.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TRANSACTIONS")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String description;
    private BigDecimal value;
    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    public void setDescription(String description) { this.description = description; }
    public void setValue(BigDecimal value) { this.value = value; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setAccount(Account account) { this.account = account; }
    
    public Long getId() { return id; }
    public String getDescription() { return description; }
    public BigDecimal getValue() { return value; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Account getAccount() { return account; }
}