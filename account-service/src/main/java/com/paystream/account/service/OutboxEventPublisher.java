package com.paystream.account.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paystream.account.domain.Account;
import com.paystream.account.domain.OutboxEvent;
import com.paystream.account.event.BalanceUpdatedEvent;
import com.paystream.account.repository.OutboxEventRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Writes {@link BalanceUpdatedEvent}s to the outbox in the caller's transaction, ensuring
 * the event is persisted atomically with the balance change it describes.
 */
@Component
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Records a balance-updated event for the given account.
     *
     * @param account the account whose balance changed (post-mutation state)
     */
    public void recordBalanceUpdated(Account account) {
        BalanceUpdatedEvent event = new BalanceUpdatedEvent(
                account.getId(), account.getOwnerId(), account.getCurrency(),
                account.getBalance(), Instant.now());
        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(new OutboxEvent(
                    "Account", account.getId().toString(), BalanceUpdatedEvent.EVENT_TYPE, payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise BalanceUpdatedEvent for " + account.getId(), e);
        }
    }
}
