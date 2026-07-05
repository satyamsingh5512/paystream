package com.paystream.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paystream.transaction.client.AccountServiceClient;
import com.paystream.transaction.domain.SagaState;
import com.paystream.transaction.domain.Transaction;
import com.paystream.transaction.dto.CreateTransactionRequest;
import com.paystream.transaction.event.FraudCheckCompletedEvent;
import com.paystream.transaction.event.FraudDecision;
import com.paystream.transaction.repository.TransactionRepository;
import com.paystream.transaction.service.TransactionService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Drives the saga end-to-end with a real Postgres and an embedded Kafka broker.
 * account-service is mocked so the test focuses on orchestration and compensation logic.
 */
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"payment.events", "fraud.alerts"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.kafka.consumer.group-id=transaction-saga",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.listener.ack-mode=manual"
})
class SagaIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("transaction_db");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @MockBean
    private AccountServiceClient accountClient; // debit/credit succeed by default (void mock)
    @MockBean
    private com.paystream.transaction.client.FraudServiceClient fraudServiceClient; // riskScore defaults to 0

    @Autowired
    private TransactionService transactionService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void happyPathCompletesTransaction() throws Exception {
        Transaction tx = transactionService.initiate(newRequest());
        assertThat(tx.getState()).isEqualTo(SagaState.DEBIT_APPLIED);

        publishFraudResult(tx.getId(), FraudDecision.APPROVED, 10);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(transactionRepository.findById(tx.getId()).orElseThrow().getState())
                        .isEqualTo(SagaState.COMPLETED));
    }

    @Test
    void flaggedTransactionIsCompensatedAndFailed() throws Exception {
        Transaction tx = transactionService.initiate(newRequest());
        assertThat(tx.getState()).isEqualTo(SagaState.DEBIT_APPLIED);

        publishFraudResult(tx.getId(), FraudDecision.FLAGGED, 85);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(transactionRepository.findById(tx.getId()).orElseThrow().getState())
                        .isEqualTo(SagaState.FAILED));
    }

    @Test
    void rejectsHighRiskUserPreEmptively() {
        // Cumulative risk score at/above the reject threshold (80) blocks initiation up front.
        when(fraudServiceClient.riskScore(any())).thenReturn(90);

        assertThatThrownBy(() -> transactionService.initiate(newRequest()))
                .isInstanceOf(com.paystream.transaction.exception.HighRiskUserException.class);
    }

    private CreateTransactionRequest newRequest() {
        return new CreateTransactionRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("250.00"), "INR", "127.0.0.1");
    }

    private void publishFraudResult(UUID txId, FraudDecision decision, int score) throws Exception {
        var event = new FraudCheckCompletedEvent(txId, UUID.randomUUID(), decision, score,
                List.of("AmountThresholdRule"), Instant.now());
        kafkaTemplate.send(MessageBuilder.withPayload(objectMapper.writeValueAsString(event))
                .setHeader(KafkaHeaders.TOPIC, "fraud.alerts")
                .setHeader(KafkaHeaders.KEY, txId.toString())
                .setHeader("eventType", FraudCheckCompletedEvent.EVENT_TYPE)
                .build());
    }
}
