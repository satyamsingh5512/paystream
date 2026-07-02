package com.paystream.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Eventually-consistent read projection of an account's balance. */
@Entity
@Table(name = "account_read_model")
public class AccountReadModel {

    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    protected AccountReadModel() {
        // JPA
    }

    public AccountReadModel(UUID accountId, UUID ownerId, String currency, BigDecimal balance) {
        this.accountId = accountId;
        this.ownerId = ownerId;
        this.currency = currency;
        this.balance = balance;
        this.lastUpdated = Instant.now();
    }

    public void apply(BigDecimal newBalance) {
        this.balance = newBalance;
        this.lastUpdated = Instant.now();
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }
}
