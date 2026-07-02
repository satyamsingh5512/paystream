package com.paystream.account.dto;

import com.paystream.account.domain.AccountReadModel;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Read-model balance view. */
public record BalanceResponse(UUID accountId, UUID ownerId, String currency, BigDecimal balance, Instant lastUpdated) {

    public static BalanceResponse from(AccountReadModel model) {
        return new BalanceResponse(model.getAccountId(), model.getOwnerId(), model.getCurrency(),
                model.getBalance(), model.getLastUpdated());
    }
}
