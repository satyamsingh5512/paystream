---
name: distributed-patterns
description: >
  Implement distributed system patterns: Saga orchestration, CQRS, Transactional Outbox,
  Circuit Breaker, idempotent Kafka consumers, optimistic locking.
---

# Distributed Patterns Implementation Guide

## 1. Transactional Outbox
```java
// Same @Transactional — write domain object + event atomically
@Transactional
public Transaction initiatePayment(CreateTransactionRequest req) {
    Transaction tx = transactionRepo.save(new Transaction(req));
    outboxRepo.save(new OutboxEvent("Transaction", tx.getId(), "payment-initiated", serialize(tx)));
    return tx;
}

// Relay — runs every 500ms
@Scheduled(fixedDelay = 500)
@Transactional
public void publishPendingEvents() {
    outboxRepo.findTop100ByPublishedFalseOrderByCreatedAtAsc()
        .forEach(event -> {
            kafkaTemplate.send(topicFor(event.getEventType()), event.getAggregateId(), event.getPayload());
            event.setPublished(true);
        });
}
```

## 2. Saga Orchestration
```java
@Service
public class SagaOrchestrator {
    public void execute(Transaction tx) {
        try {
            updateState(tx, DEBIT_PENDING);
            accountService.debit(tx.getSourceAccountId(), tx.getAmount());
            updateState(tx, DEBIT_APPLIED);
            publishFraudCheckRequest(tx); // Async — saga pauses, resumes in listener below
        } catch (Exception e) {
            compensate(tx);
        }
    }

    @KafkaListener(topics = "fraud-check-completed")
    public void onFraudCheckCompleted(FraudCheckCompletedEvent event) {
        Transaction tx = transactionRepo.findById(event.transactionId()).orElseThrow();
        if (event.decision() == APPROVED) {
            try {
                accountService.credit(tx.getDestAccountId(), tx.getAmount());
                updateState(tx, COMPLETED);
                publishPaymentCompleted(tx);
            } catch (Exception e) {
                compensate(tx);
            }
        } else {
            compensate(tx);
        }
    }

    private void compensate(Transaction tx) {
        if (tx.getState().ordinal() >= DEBIT_APPLIED.ordinal()) {
            accountService.credit(tx.getSourceAccountId(), tx.getAmount()); // Reverse debit
        }
        updateState(tx, FAILED);
        publishPaymentFailed(tx);
    }
}
```

## 3. CQRS Account Service
```java
// Write side — strict consistency with optimistic locking
@Transactional
public void debit(UUID accountId, BigDecimal amount) {
    Account account = accountRepo.findById(accountId)
        .orElseThrow(() -> new EntityNotFoundException("Account: " + accountId));
    if (account.getBalance().compareTo(amount) < 0)
        throw new InsufficientBalanceException(accountId, amount);
    account.setBalance(account.getBalance().subtract(amount));
    // @Version auto-incremented — throws OptimisticLockException on conflict
    outboxRepo.save(new OutboxEvent("Account", accountId.toString(), "balance-updated", serialize(account)));
}

// Read side — eventual consistency, no write locks
@KafkaListener(topics = "payment.events", groupId = "account-read-model-updater")
public void updateReadModel(BalanceUpdatedEvent event) {
    accountReadModelRepo.updateBalance(event.accountId(), event.newBalance());
}
```

## 4. Idempotent Kafka Consumer
```java
@KafkaListener(topics = "payment.events", groupId = "ledger-group")
@Transactional
public void consume(ConsumerRecord<String, String> record) {
    String messageId = new String(record.headers().lastHeader("messageId").value());
    if (idempotencyRepo.existsById(messageId)) return; // Already processed
    PaymentEvent event = deserialize(record.value());
    processEvent(event);
    idempotencyRepo.save(new IdempotencyKey(messageId, Instant.now()));
}
```

## 5. Resilience4j Circuit Breaker
```java
@CircuitBreaker(name = "account-service", fallbackMethod = "debitFallback")
@Retry(name = "account-service")
@Bulkhead(name = "account-service")
public DebitResponse debit(DebitRequest request) {
    return accountServiceClient.post()
        .uri("/api/v1/accounts/debit")
        .body(request)
        .retrieve()
        .body(DebitResponse.class);
}

public DebitResponse debitFallback(DebitRequest request, Exception ex) {
    log.error("Circuit open. Queuing for manual review: {}", request.transactionId());
    manualReviewQueue.enqueue(request);
    throw new ServiceUnavailableException("Payment processing temporarily delayed");
}
```
