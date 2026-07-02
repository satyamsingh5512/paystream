package com.paystream.account.exception;

import java.util.UUID;

/** Thrown when an account id cannot be resolved (HTTP 404). */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(UUID accountId) {
        super("Account not found: " + accountId);
    }
}
