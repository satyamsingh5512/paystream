package com.paystream.account.exception;

import java.math.BigDecimal;
import java.util.UUID;

/** Thrown when a debit would overdraw an account (HTTP 422). */
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(UUID accountId, BigDecimal amount) {
        super("Failed to debit account %s: insufficient balance for amount %s".formatted(accountId, amount));
    }
}
