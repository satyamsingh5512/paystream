package com.paystream.account.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Emitted whenever an account balance changes. Consumed by the read-model projection and
 * (downstream) by the ledger service.
 *
 * @param accountId  the affected account
 * @param ownerId    the account owner
 * @param currency   ISO currency code
 * @param newBalance the post-mutation balance
 * @param occurredAt event timestamp
 */
public record BalanceUpdatedEvent(
        UUID accountId,
        UUID ownerId,
        String currency,
        BigDecimal newBalance,
        Instant occurredAt) {

    public static final String EVENT_TYPE = "balance-updated";
}
