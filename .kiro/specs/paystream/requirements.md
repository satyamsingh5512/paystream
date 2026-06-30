# PayStream – Requirements

## Overview
PayStream is an event-driven microservices platform for real-time payment processing, fraud detection, and financial reconciliation built using Spring Boot 3, Apache Kafka, Redis, and React 18.

## EARS Requirements

### Authentication
WHEN a user submits valid credentials
THE SYSTEM SHALL issue a signed JWT access token (15-min TTL) and refresh token (7-day TTL) in HttpOnly cookie.

WHEN a user submits an expired access token with a valid refresh token
THE SYSTEM SHALL issue a new access token without requiring re-login.

WHEN a user submits invalid credentials three times consecutively
THE SYSTEM SHALL lock the account for 15 minutes and return HTTP 423.

### Account Management
THE SYSTEM SHALL maintain a separate read model for account balances updated via Kafka events (CQRS).

WHEN a balance mutation occurs
THE SYSTEM SHALL publish a `balance-updated` event to Kafka in the same DB transaction using Transactional Outbox.

WHILE two concurrent transactions attempt to modify the same account balance
THE SYSTEM SHALL use optimistic locking (@Version) and retry on OptimisticLockException up to 3 times.

### Payment Processing
WHEN a user initiates a payment via POST /api/v1/transactions
THE SYSTEM SHALL execute a saga: (1) debit source, (2) fraud check, (3) credit destination, (4) notify.

WHEN any saga step fails
THE SYSTEM SHALL execute compensating transactions for all previously completed steps and set status to FAILED.

WHEN a payment saga completes successfully
THE SYSTEM SHALL push a WebSocket notification to the initiating user within 500ms.

### Fraud Detection
WHEN a payment-initiated event is consumed
THE SYSTEM SHALL evaluate: velocity check (>10 txns/min in Redis), geo-anomaly (IP mismatch), amount threshold (>₹5,00,000).

WHEN a transaction is flagged
THE SYSTEM SHALL publish fraud-alert-raised, update status to FLAGGED, increment user fraud score in Redis.

WHILE a user's fraud score exceeds 80
THE SYSTEM SHALL reject all new payment initiations from that user with HTTP 403.

### Notifications
WHEN a payment-completed or fraud-alert-raised event is consumed
THE SYSTEM SHALL push a WebSocket message to the connected client within 200ms.

IF the user is not connected via WebSocket
THE SYSTEM SHALL send an email notification via SMTP within 30 seconds.

### Ledger
WHEN any payment event is consumed by the ledger service
THE SYSTEM SHALL write an immutable double-entry ledger record that cannot be updated or deleted.

WHEN the system clock reaches 00:00 UTC
THE SYSTEM SHALL generate a daily reconciliation report at GET /api/v1/ledger/reports/{date}.

### Resilience
WHEN the fraud-service circuit breaker opens (>50% failure rate in 10s)
THE SYSTEM SHALL route transactions to a fallback manual review queue.

WHEN any downstream call exceeds 3 seconds
THE SYSTEM SHALL time out and retry (3 retries, 500ms exponential backoff).

### Observability
THE SYSTEM SHALL expose Prometheus metrics at /actuator/prometheus for every service.
THE SYSTEM SHALL include X-Correlation-ID on all inter-service requests.
THE SYSTEM SHALL emit structured JSON logs with: timestamp, level, service, traceId, spanId, message.
