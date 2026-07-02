package com.paystream.account.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Debit/credit instruction issued by the transaction-service saga.
 *
 * @param transactionId the owning saga transaction (for traceability/idempotency)
 * @param amount        positive monetary amount
 */
public record MoneyOperationRequest(@NotNull UUID transactionId, @NotNull @Positive BigDecimal amount) {
}
