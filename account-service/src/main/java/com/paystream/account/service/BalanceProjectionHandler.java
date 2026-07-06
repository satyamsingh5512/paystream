package com.paystream.account.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paystream.account.config.KafkaTopicConfig;
import com.paystream.account.domain.AccountReadModel;
import com.paystream.account.event.BalanceUpdatedEvent;
import com.paystream.account.repository.AccountReadModelRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Projects {@link BalanceUpdatedEvent}s onto the read model (CQRS). Upserts the projection
 * row so the read side becomes eventually consistent with the write side.
 */
@Component
public class BalanceProjectionHandler {

    private static final Logger log = LoggerFactory.getLogger(BalanceProjectionHandler.class);

    private final AccountReadModelRepository readModelRepository;
    private final ObjectMapper objectMapper;

    public BalanceProjectionHandler(AccountReadModelRepository readModelRepository, ObjectMapper objectMapper) {
        this.readModelRepository = readModelRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopicConfig.PAYMENT_EVENTS, groupId = "${spring.kafka.consumer.group-id:account-read-model-updater}")
    @Transactional
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            var typeHeader = record.headers().lastHeader("eventType");
            String eventType = typeHeader == null ? null : new String(typeHeader.value());
            if (!BalanceUpdatedEvent.EVENT_TYPE.equals(eventType)) {
                ack.acknowledge();
                return;
            }
            BalanceUpdatedEvent event = objectMapper.readValue(record.value(), BalanceUpdatedEvent.class);
            AccountReadModel model = readModelRepository.findById(event.accountId())
                    .map(existing -> {
                        existing.apply(event.newBalance());
                        return existing;
                    })
                    .orElseGet(() -> new AccountReadModel(
                            event.accountId(), event.ownerId(), event.currency(), event.newBalance()));
            readModelRepository.save(model);
            ack.acknowledge();
        } catch (Exception e) {
            // Do not acknowledge — the record will be redelivered. Idempotent upsert makes this safe.
            log.error("Failed to project balance-updated event: {}", e.getMessage(), e);
            throw new IllegalStateException("Projection failed", e);
        }
    }
}
