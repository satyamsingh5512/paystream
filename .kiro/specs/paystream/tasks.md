# PayStream – Implementation Tasks

## Phase 1: Infrastructure Foundation
- [x] 1.1 Create root pom.xml with Spring Boot 3.3 BOM, Java 21, common dependency versions
- [x] 1.2 Create docker-compose.yml: PostgreSQL (5 DBs — notification is stateless), Redis, Kafka+Zookeeper, Prometheus, Grafana, Loki
- [x] 1.3 Create config-server module — Spring Cloud Config Server
- [x] 1.4 Create application.yml configs for every service under config-server/src/main/resources/config/
- [x] 1.5 Create api-gateway module — Spring Cloud Gateway with Eureka client, JWT filter, Redis rate limiter
- [x] 1.6 Set up Eureka server (standalone eureka-server module, port 8761)
- [x] 1.7 Create .env.example with all environment variables documented

## Phase 2: Auth Service
- [x] 2.1 Create auth-service module with Spring Security 6, JPA, Flyway
- [x] 2.2 Flyway migrations: V1__create_users.sql, V2__create_refresh_tokens.sql
- [x] 2.3 Implement User entity: id, email, passwordHash, role, locked, failedAttempts
- [x] 2.4 Implement JwtTokenProvider — RSA-256 sign/verify; generateAccessToken(), generateRefreshToken(), validateToken()
- [x] 2.5 Implement AuthController — /auth/register, /auth/login, /auth/refresh, /auth/logout
- [x] 2.6 Implement LoginAttemptService — Redis-backed, locks after 3 failures, TTL 15 min
- [x] 2.7 Implement TokenBlacklistService — Redis SET for JTI invalidation
- [x] 2.8 Unit tests for JwtTokenProvider and LoginAttemptService
- [x] 2.9 Integration test for /auth/login using Testcontainers (PostgreSQL + Redis)
- [x] 2.10 OpenAPI annotations on all endpoints

## Phase 3: Account Service
- [x] 3.1 Create account-service module
- [x] 3.2 Flyway migrations: V1__create_accounts.sql, V2__create_outbox_events.sql, V3__create_read_model.sql
- [x] 3.3 Implement Account entity with @Version optimistic locking; balance as BigDecimal
- [x] 3.4 Implement AccountCommandService — debit() and credit() with 3-attempt retry on OptimisticLockException
- [x] 3.5 Implement OutboxEventPublisher — write event to outbox_events in same transaction
- [x] 3.6 Implement OutboxRelay — @Scheduled(fixedDelay=500) reads+publishes unpublished outbox events
- [x] 3.7 Implement AccountQueryService + BalanceProjectionHandler (Kafka consumer)
- [x] 3.8 Implement AccountController — create, get balance (from read model), get details
- [x] 3.9 Integration test: 10-thread concurrent debit stress test — assert no balance inconsistency
- [x] 3.10 Resilience4j circuit breaker on all outbound RestClient calls (N/A: account-service has no outbound calls; the breaker lives in transaction-service which calls it — see Phase 4)

## Phase 4: Transaction Service + Saga
- [x] 4.1 Create transaction-service module
- [x] 4.2 Flyway migrations: V1__create_transactions.sql, V2__create_saga_state_log.sql (+ V3 outbox)
- [x] 4.3 Implement Transaction entity; SagaState enum; SagaStateLog for audit
- [x] 4.4 Implement SagaOrchestrator — executes steps in sequence, saves state after each
- [x] 4.5 Implement DebitSourceStep — Resilience4j circuit-broken RestClient call to account-service (AccountServiceClient)
- [x] 4.6 Kafka producer for payment-fraud-check-requested; consumer for fraud-check-completed (outbox payment-initiated; FraudCheckListener)
- [x] 4.7 Implement CreditDestinationStep — executes only if fraud check = APPROVED
- [x] 4.8 Implement compensation logic — ReverseDebitStep on fraud rejection or credit failure
- [x] 4.9 Implement TransactionController — POST /api/v1/transactions, GET /{id}, GET (paginated)
- [x] 4.10 Integration test: full saga happy path (Testcontainers Postgres + EmbeddedKafka, mocked account client)
- [x] 4.11 Integration test: saga compensation path (mock fraud returning FLAGGED)

## Phase 5: Fraud Service
- [x] 5.1 Create fraud-service module (Kafka consumer only)
- [x] 5.2 Flyway migrations: V1__create_fraud_events.sql, V2__create_user_risk_scores.sql
- [x] 5.3 Implement FraudRule interface: evaluate(PaymentEvent): RuleResult
- [x] 5.4 Implement VelocityRule — Redis INCR, 60s TTL, configurable threshold
- [x] 5.5 Implement AmountThresholdRule — configurable max; default ₹5,00,000
- [x] 5.6 Implement GeoAnomalyRule — pluggable GeoIpResolver (stub fail-open; swap MaxMind for prod)
- [x] 5.7 Implement FraudScoringEngine — runs all rules, computes score, returns decision
- [x] 5.8 Implement FraudEventConsumer — @KafkaListener; invoke engine; publish fraud-check-completed
- [x] 5.9 Unit tests for each FraudRule
- [x] 5.10 Integration test with @EmbeddedKafka: consume → evaluate → publish flow

## Phase 6: Notification Service
- [x] 6.1 Create notification-service with spring-boot-starter-websocket, spring-boot-starter-mail
- [x] 6.2 Configure Spring WebSocket + STOMP; endpoint /ws; topic /topic
- [x] 6.3 Implement WebSocketSessionStore — Redis presence map: userId <-> sessionId
- [x] 6.4 Implement PaymentEventConsumer — @KafkaListener on payment.events (filter: payment-completed)
- [x] 6.5 Implement FraudAlertConsumer — @KafkaListener on fraud.alerts
- [x] 6.6 Implement NotificationDispatcher — WS push if session active; email fallback
- [x] 6.7 Thymeleaf email templates: payment-completed.html, fraud-alert.html
- [x] 6.8 Integration test for WebSocket push using StompClient

## Phase 7: Ledger Service
- [x] 7.1 Create ledger-service module
- [x] 7.2 Flyway migrations: V1__create_ledger_entries.sql (INSERT-only), V2__create_reconciliation_reports.sql
- [x] 7.3 Implement LedgerEntry entity — immutable (no setters)
- [x] 7.4 DB trigger in migration to block UPDATE/DELETE on ledger_entries
- [x] 7.5 Implement LedgerEventConsumer — consumes all payment.events; writes double-entry pairs
- [x] 7.6 Implement ReconciliationScheduler — @Scheduled(cron="0 0 0 * * *"); persists daily report
- [x] 7.7 Implement LedgerController — GET /api/v1/ledger/entries (paginated), GET /reports/{date}
- [x] 7.8 Integration test: verify double-entry invariant (debit + credit always sum to zero) + trigger blocks update

## Phase 8: React Frontend
- [x] 8.1 Scaffold Vite + React 18 + TypeScript; install Tailwind CSS 3, shadcn/ui
- [x] 8.2 Configure Tailwind with PayStream design tokens
- [x] 8.3 Install JetBrains Mono (Fontsource); configure Inter as body font
- [x] 8.4 Create AppShell.tsx, Sidebar.tsx, TopHeader.tsx layout components
- [x] 8.5 Implement useAuthStore.ts — Zustand: access token, user info, expiry, refresh handler
- [x] 8.6 Implement apiClient.ts — Axios with Bearer token + 401 → refresh → retry interceptor
- [x] 8.7 Implement websocketClient.ts — STOMP/SockJS; auto-reconnect; dispatch to Zustand store
- [x] 8.8 Build LoginPage.tsx — centered card, React Hook Form + Zod, error toasts
- [x] 8.9 Build DashboardPage.tsx — 4 KPI cards + area chart (Recharts)
- [x] 8.10 Build TransactionsPage.tsx — sortable/filterable data table, pagination, row click → detail drawer
- [x] 8.11 Build TransactionDetailDrawer.tsx — saga timeline, amount, counterparty, ledger entries
- [x] 8.12 Build FraudAlertsPage.tsx — real-time alert feed, severity badge, triggered rules, actions
- [x] 8.13 Build LiveFeedPanel.tsx — WebSocket-powered feed, new items slide from top
- [x] 8.14 Build AccountsPage.tsx — balance in JetBrains Mono, mini area chart per account
- [x] 8.15 Build LedgerPage.tsx — ADMIN only; date picker for reconciliation report; CSV export
- [x] 8.16 Build AdminPage.tsx — user management table, transaction lookup, manual freeze toggle
- [x] 8.17 Protected routes with role guards (USER vs ADMIN); redirect to login on 401
- [x] 8.18 Framer Motion AnimatePresence page transitions (150ms fade)
- [x] 8.19 react-hot-toast WebSocket notifications (bottom-right, non-blocking)
- [x] 8.20 CSV export utility for transactions and ledger tables
- [x] 8.21 Vitest + React Testing Library tests for LoginPage, TransactionsPage, useAuthStore

## Phase 9: Observability
- [x] 9.1 Add micrometer-registry-prometheus to all services; configure /actuator/prometheus
- [x] 9.2 Configure prometheus.yml to scrape all 6 service endpoints (+ platform)
- [x] 9.3 Import Grafana Spring Boot dashboard (ID 11378) — provisioning folder ready; import via Grafana UI/API on first boot
- [x] 9.4 Create custom PayStream Grafana dashboard: throughput, error rate, p95, circuit breaker, fraud rate, JVM heap
- [x] 9.5 Configure Logback JSON output (logstash-logback-encoder) in all services + gateway
- [x] 9.6 Add X-Correlation-ID filter in Gateway (CorrelationIdGlobalFilter); propagate via MDC in all services
- [x] 9.7 Actuator health groups: liveness and readiness (shared config/application.yml)

## Phase 10: Polish & Docs
- [x] 10.1 README.md — Mermaid architecture diagram, quick start, API overview
- [x] 10.2 Export OpenAPI YAML from each service → docs/api-contracts/ (export instructions + URLs documented)
- [x] 10.3 ADRs: adr-001-kafka-over-rest.md, adr-002-outbox-pattern.md, adr-003-cqrs-account-service.md
- [x] 10.4 GitHub Actions CI: build all Maven modules, run tests, lint frontend, build Docker images on main
- [x] 10.5 CONTRIBUTING.md with branch naming, PR template, code review checklist
