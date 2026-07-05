package com.paystream.transaction.service;

import com.paystream.transaction.client.AccountServiceClient;
import com.paystream.transaction.domain.SagaState;
import com.paystream.transaction.domain.SagaStateLog;
import com.paystream.transaction.domain.Transaction;
import com.paystream.transaction.event.FraudCheckCompletedEvent;
import com.paystream.transaction.event.FraudDecision;
import com.paystream.transaction.event.PaymentCompletedEvent;
import com.paystream.transaction.event.PaymentFailedEvent;
import com.paystream.transaction.event.PaymentInitiatedEvent;
import com.paystream.transaction.exception.AccountOperationException;
import com.paystream.transaction.exception.ServiceUnavailableException;
import com.paystream.transaction.repository.SagaStateLogRepository;
import com.paystream.transaction.repository.TransactionRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the payment saga.
 *
 * <pre>
 *   start():        debit source  -> DEBIT_APPLIED -> publish payment-initiated (fraud check is async)
 *   onFraudCheck(): APPROVED -> credit destination -> COMPLETED -> publish payment-completed
 *                   FLAGGED  -> compensate (reverse debit) -> FAILED -> publish payment-failed
 *   compensate():   reverse the source debit when it had been applied
 * </pre>
 */
@Service
public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

    private final TransactionRepository transactionRepository;
    private final SagaStateLogRepository sagaStateLogRepository;
    private final AccountServiceClient accountClient;
    private final OutboxEventPublisher outbox;

    public SagaOrchestrator(TransactionRepository transactionRepository,
                            SagaStateLogRepository sagaStateLogRepository,
                            AccountServiceClient accountClient,
                            OutboxEventPublisher outbox) {
        this.transactionRepository = transactionRepository;
        this.sagaStateLogRepository = sagaStateLogRepository;
        this.accountClient = accountClient;
        this.outbox = outbox;
    }

    /** Step 1: debit the source account, then hand off to the async fraud check. */
    @Transactional
    public void start(Transaction tx) {
        try {
            accountClient.debit(tx.getSourceAccountId(), tx.getId(), tx.getAmount());
            transition(tx, SagaState.DEBIT_APPLIED, "Source account debited");
            outbox.publish(tx.getId().toString(), PaymentInitiatedEvent.EVENT_TYPE,
                    new PaymentInitiatedEvent(tx.getId(), tx.getUserId(), tx.getSourceAccountId(),
                            tx.getDestAccountId(), tx.getAmount(), tx.getCurrency(), tx.getIpAddress(), Instant.now()));
        } catch (AccountOperationException | ServiceUnavailableException e) {
            fail(tx, "Debit failed: " + e.getMessage());
        }
    }

    /** Step 2 (resumed from Kafka): apply the fraud decision. */
    @Transactional
    public void onFraudCheckCompleted(FraudCheckCompletedEvent event) {
        Transaction tx = transactionRepository.findById(event.transactionId()).orElse(null);
        if (tx == null) {
            log.warn("Fraud result for unknown transaction {}", event.transactionId());
            return;
        }
        if (tx.getState() != SagaState.DEBIT_APPLIED) {
            // Already processed or not in a resumable state — idempotent no-op.
            log.debug("Ignoring fraud result for tx {} in state {}", tx.getId(), tx.getState());
            return;
        }
        transition(tx, SagaState.FRAUD_CHECKED, "Fraud decision: " + event.decision());

        if (event.decision() == FraudDecision.APPROVED) {
            try {
                accountClient.credit(tx.getDestAccountId(), tx.getId(), tx.getAmount());
                transition(tx, SagaState.COMPLETED, "Destination account credited");
                outbox.publish(tx.getId().toString(), PaymentCompletedEvent.EVENT_TYPE,
                        new PaymentCompletedEvent(tx.getId(), tx.getUserId(), tx.getSourceAccountId(),
                                tx.getDestAccountId(), tx.getAmount(), tx.getCurrency(), Instant.now()));
            } catch (AccountOperationException | ServiceUnavailableException e) {
                compensate(tx, "Credit failed: " + e.getMessage());
            }
        } else {
            compensate(tx, "Flagged by fraud (score=%d)".formatted(event.score()));
        }
    }

    private void compensate(Transaction tx, String reason) {
        if (tx.isDebitApplied()) {
            try {
                // Reverse the source debit.
                accountClient.credit(tx.getSourceAccountId(), tx.getId(), tx.getAmount());
                log.info("Compensated tx {}: reversed source debit", tx.getId());
            } catch (RuntimeException e) {
                // Compensation itself failed — record for manual reconciliation.
                log.error("Compensation FAILED for tx {}: {}", tx.getId(), e.getMessage());
                reason = reason + "; compensation failed: " + e.getMessage();
            }
        }
        fail(tx, reason);
    }

    private void fail(Transaction tx, String reason) {
        tx.markFailed(reason);
        transactionRepository.save(tx);
        sagaStateLogRepository.save(new SagaStateLog(tx.getId(), SagaState.FAILED, reason));
        outbox.publish(tx.getId().toString(), PaymentFailedEvent.EVENT_TYPE,
                new PaymentFailedEvent(tx.getId(), tx.getUserId(), tx.getAmount(), tx.getCurrency(), reason, Instant.now()));
    }

    private void transition(Transaction tx, SagaState state, String detail) {
        tx.transitionTo(state);
        transactionRepository.save(tx);
        sagaStateLogRepository.save(new SagaStateLog(tx.getId(), state, detail));
    }
}
