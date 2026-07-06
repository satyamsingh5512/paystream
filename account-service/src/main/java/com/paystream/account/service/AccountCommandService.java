package com.paystream.account.service;

import com.paystream.account.config.AccountProperties;
import com.paystream.account.domain.Account;
import com.paystream.account.dto.CreateAccountRequest;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * Command side of the CQRS account model. Retries balance mutations on optimistic-lock
 * conflicts up to the configured maximum, satisfying the requirement that concurrent
 * updates never corrupt a balance.
 */
@Service
public class AccountCommandService {

    private static final Logger log = LoggerFactory.getLogger(AccountCommandService.class);

    private final AccountBalanceMutator mutator;
    private final int maxRetries;

    public AccountCommandService(AccountBalanceMutator mutator, AccountProperties properties) {
        this.mutator = mutator;
        this.maxRetries = properties.optimisticLock().maxRetries();
    }

    public Account create(CreateAccountRequest request) {
        return mutator.create(request);
    }

    /** Debits an account, retrying on optimistic-lock conflicts. */
    public Account debit(UUID accountId, BigDecimal amount) {
        return withRetry(() -> mutator.applyDebit(accountId, amount), "debit", accountId);
    }

    /** Credits an account, retrying on optimistic-lock conflicts. */
    public Account credit(UUID accountId, BigDecimal amount) {
        return withRetry(() -> mutator.applyCredit(accountId, amount), "credit", accountId);
    }

    private Account withRetry(java.util.function.Supplier<Account> op, String action, UUID accountId) {
        int attempt = 0;
        while (true) {
            try {
                return op.get();
            } catch (ObjectOptimisticLockingFailureException ex) {
                attempt++;
                if (attempt >= maxRetries) {
                    log.error("Exhausted {} retries on {} for account {}", maxRetries, action, accountId);
                    throw ex;
                }
                log.warn("Optimistic lock conflict on {} for account {} (attempt {}/{})",
                        action, accountId, attempt, maxRetries);
            }
        }
    }
}
