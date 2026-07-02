package com.paystream.account.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/** Request to open a new account. */
public record CreateAccountRequest(
        @NotNull UUID ownerId,
        @Size(min = 3, max = 3) String currency,
        @PositiveOrZero BigDecimal openingBalance) {

    public String currencyOrDefault() {
        return currency == null || currency.isBlank() ? "INR" : currency;
    }

    public BigDecimal openingOrZero() {
        return openingBalance == null ? BigDecimal.ZERO : openingBalance;
    }
}
