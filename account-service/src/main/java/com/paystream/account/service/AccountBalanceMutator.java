package com.paystream.account.service;

import com.paystream.account.domain.Account;
import com.paystream.account.dto.CreateAccountRequest;
import com.paystream.account.exception.AccountNotFoundException;
import com.paystream.account.exception.InsufficientBalanceException;
import com.paystream.account.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Performs the actual transactional balance mutations. Lives in its own bean so that each
 * call runs in a fresh transaction whose commit (and therefore optimistic-lock check) is
 * observable by the retrying {@link AccountCommandService}.
 */
@Component
public class AccountBalanceMutator {

    private final AccountRepository accountRepository;
    private final OutboxEventPublisher outboxPublisher;

    public AccountBalanceMutator(AccountRepository accountRepository, OutboxEventPublisher outboxPublisher) {
        this.accountRepository = accountRepository;
        this.outboxPublisher = outboxPublisher;
    }

    /** Creates a new account and seeds its read model via an outbox event. */
    @Transactional
    public Account create(CreateAccountRequest request) {
        Account account = accountRepository.save(
                new Account(request.ownerId(), request.currencyOrDefault(), request.openingOrZero()));
        outboxPublisher.recordBalanceUpdated(account);
        return account;
    }

    /**
     * Debits an account within a single transaction.
     *
     * @throws AccountNotFoundException   if the account does not exist
     * @throws InsufficientBalanceException if the balance is insufficient
     */
    @Transactional
    public Account applyDebit(UUID accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(accountId, amount);
        }
        account.debit(amount);
        outboxPublisher.recordBalanceUpdated(account);
        return account;
    }

    /** Credits an account within a single transaction. */
    @Transactional
    public Account applyCredit(UUID accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        account.credit(amount);
        outboxPublisher.recordBalanceUpdated(account);
        return account;
    }
}
