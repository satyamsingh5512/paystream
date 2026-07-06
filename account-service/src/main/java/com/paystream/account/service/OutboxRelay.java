package com.paystream.account.service;

import com.paystream.account.config.KafkaTopicConfig;
import com.paystream.account.domain.OutboxEvent;
import com.paystream.account.repository.OutboxEventRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relays unpublished outbox rows to Kafka on a fixed schedule, marking them published in
 * the same transaction. At-least-once delivery; consumers must be idempotent.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelay(OutboxEventRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${paystream.account.outbox.relay-fixed-delay-ms:500}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();
        if (pending.isEmpty()) {
            return;
        }
        for (OutboxEvent event : pending) {
            var message = MessageBuilder.withPayload(event.getPayload())
                    .setHeader(KafkaHeaders.TOPIC, KafkaTopicConfig.PAYMENT_EVENTS)
                    .setHeader(KafkaHeaders.KEY, event.getAggregateId())
                    .setHeader("eventType", event.getEventType())
                    .setHeader("messageId", event.getId().toString())
                    .build();
            kafkaTemplate.send(message);
            event.markPublished();
        }
        log.debug("Relayed {} outbox event(s) to Kafka", pending.size());
    }
}
