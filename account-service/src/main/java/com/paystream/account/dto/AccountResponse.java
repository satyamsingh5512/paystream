package com.paystream.account.dto;

import com.paystream.account.domain.Account;
import java.math.BigDecimal;
import java.util.UUID;

/** Write-model account view. */
public record AccountResponse(UUID id, UUID ownerId, String currency, BigDecimal balance, long version) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getOwnerId(), account.getCurrency(),
                account.getBalance(), account.getVersion());
    }
}
