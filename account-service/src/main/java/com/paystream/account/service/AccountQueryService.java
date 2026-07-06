package com.paystream.account.service;

import com.paystream.account.domain.AccountReadModel;
import com.paystream.account.exception.AccountNotFoundException;
import com.paystream.account.repository.AccountReadModelRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Query side of the CQRS account model — lock-free reads from the projection table. */
@Service
@Transactional(readOnly = true)
public class AccountQueryService {

    private final AccountReadModelRepository readModelRepository;

    public AccountQueryService(AccountReadModelRepository readModelRepository) {
        this.readModelRepository = readModelRepository;
    }

    public AccountReadModel getBalance(UUID accountId) {
        return readModelRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    public List<AccountReadModel> getByOwner(UUID ownerId) {
        return readModelRepository.findByOwnerId(ownerId);
    }
}
