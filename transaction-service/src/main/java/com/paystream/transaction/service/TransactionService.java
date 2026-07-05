package com.paystream.transaction.service;

import com.paystream.transaction.client.FraudServiceClient;
import com.paystream.transaction.domain.SagaState;
import com.paystream.transaction.domain.SagaStateLog;
import com.paystream.transaction.domain.Transaction;
import com.paystream.transaction.dto.CreateTransactionRequest;
import com.paystream.transaction.exception.HighRiskUserException;
import com.paystream.transaction.exception.TransactionNotFoundException;
import com.paystream.transaction.repository.SagaStateLogRepository;
import com.paystream.transaction.repository.TransactionRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for creating and querying transactions. */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final SagaStateLogRepository sagaStateLogRepository;
    private final SagaOrchestrator orchestrator;
    private final FraudServiceClient fraudServiceClient;
    private final int rejectThreshold;

    public TransactionService(TransactionRepository transactionRepository,
                              SagaStateLogRepository sagaStateLogRepository,
                              SagaOrchestrator orchestrator,
                              FraudServiceClient fraudServiceClient,
                              @Value("${paystream.transaction.fraud-reject-threshold:80}") int rejectThreshold) {
        this.transactionRepository = transactionRepository;
        this.sagaStateLogRepository = sagaStateLogRepository;
        this.orchestrator = orchestrator;
        this.fraudServiceClient = fraudServiceClient;
        this.rejectThreshold = rejectThreshold;
    }

    /**
     * Persists a new transaction and kicks off the saga (debit + fraud-check publish).
     *
     * <p>Before doing any work, high-risk users (cumulative score at/above the reject
     * threshold) are rejected pre-emptively with {@link HighRiskUserException} (HTTP 403).
     *
     * @param request the payment instruction
     * @return the persisted transaction with its post-start state
     */
    @Transactional
    public Transaction initiate(CreateTransactionRequest request) {
        UUID userId = request.userId();
        if (userId != null) {
            int score = fraudServiceClient.riskScore(userId);
            if (score >= rejectThreshold) {
                throw new HighRiskUserException(userId, score, rejectThreshold);
            }
        }
        Transaction tx = new Transaction(userId, request.sourceAccountId(), request.destAccountId(),
                request.amount(), request.currencyOrDefault(), request.ipAddress());
        transactionRepository.save(tx);
        sagaStateLogRepository.save(new SagaStateLog(tx.getId(), SagaState.INITIATED, "Transaction created"));
        orchestrator.start(tx);
        return tx;
    }

    @Transactional(readOnly = true)
    public Transaction getById(UUID id) {
        return transactionRepository.findById(id).orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Transaction> list(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }
}
