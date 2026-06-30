# PayStream – Technical Design

## Architecture
```
Client (React) ──HTTPS──► API Gateway (8080)
                                │
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                         ▼
   auth-service          account-service         transaction-service
     (8081)                 (8082)                    (8083)
                                                          │
                              ┌───────────────────────────┤
                              ▼                           ▼
                        fraud-service           notification-service
                          (8084)                     (8085) ──WS──► Client
                              │
                              ▼
                        ledger-service
                          (8086)

All services ──publish/subscribe──► Apache Kafka
All services ──register──► Eureka (8761)
All services ──pull config──► Config Server (8888)
```

## Service Design

### auth-service
- `AuthController` – /auth/register, /auth/login, /auth/refresh, /auth/logout
- `JwtTokenProvider` – RSA-256 sign/verify; public key at /.well-known/jwks.json
- `TokenBlacklistService` – Redis SET of invalidated JTIs
- `LoginAttemptService` – Redis counter with TTL; locks after 3 failures
- DB: `auth_db` → tables: users, roles, refresh_tokens

### account-service (CQRS)
- Write: `AccountCommandService` + `@Version` optimistic locking + OutboxEventPublisher
- Read: `AccountQueryService` from `account_read_model` table
- `BalanceProjectionHandler` – Kafka consumer updating read model
- DB: `account_db` → tables: accounts, outbox_events, account_read_model

### transaction-service (Saga Orchestrator)
- Steps: DebitSource → FraudCheck (async via Kafka) → CreditDestination → Notify
- Compensation: ReverseDebitStep on any failure after debit applied
- SagaState enum: INITIATED → DEBIT_APPLIED → FRAUD_CHECKED → COMPLETED / FAILED
- DB: `transaction_db` → tables: transactions, saga_state_log, outbox_events

### fraud-service
- FraudRule interface: VelocityRule (Redis), GeoAnomalyRule (MaxMind), AmountThresholdRule
- Scoring: velocity=40pts, geo=35pts, amount=25pts; score ≥50 → flagged
- Publishes: FraudCheckCompletedEvent with approved/flagged + triggered rules
- DB: `fraud_db` → tables: fraud_events, user_risk_scores

### notification-service
- Spring WebSocket + STOMP; topic /topic/payments/{userId}
- Redis HASH ws:sessions maps userId → sessionId
- Kafka consumers on payment.events and fraud.alerts
- Email fallback via Spring Mail + Thymeleaf templates
- DB: stateless (Redis only)

### ledger-service
- INSERT-only LedgerEntry rows; DB trigger blocks UPDATE/DELETE
- Double-entry: every payment = one DEBIT + one CREDIT row with same transactionId
- Nightly @Scheduled reconciliation report; cached in Redis 24h
- DB: `ledger_db` → tables: ledger_entries, reconciliation_reports

## Kafka Topics
| Topic | Partitions | Retention | Key |
|---|---|---|---|
| payment.events | 6 | 7 days | transactionId |
| fraud.alerts | 3 | 30 days | userId |
| notification.events | 3 | 1 day | userId |

## Security
- RSA-256 JWTs. Gateway validates; injects X-User-Id header downstream.
- CORS: localhost:5173 (dev) + production domain only.
- BCrypt cost factor 12. AES-256 encryption on PII columns.

## Happy Path Data Flow
```
POST /api/v1/transactions
→ Gateway (JWT + rate limit)
→ transaction-service: INSERT transaction + outbox_event
→ Kafka: payment-initiated
→ account-service: debit (optimistic lock)
→ fraud-service: evaluate rules → fraud-check-completed
→ transaction-service: credit destination
→ Kafka: payment-completed
→ notification-service: WebSocket push
→ ledger-service: INSERT two LedgerEntry rows
```
