---
inclusion: always
---

# PayStream – Product North Star

## Vision
PayStream is a **production-grade, event-driven payment processing platform** built by a senior fintech engineering team. Every decision — architecture, UI, error handling, naming — must reflect the standards of a Tier-1 financial technology company (think Stripe, Razorpay, or Adyen).

## Core Product Pillars
1. **Reliability** – Zero message loss via Transactional Outbox. Saga compensation on every failure path.
2. **Observability** – Every service emits structured logs, metrics, and distributed traces. Nothing is a black box.
3. **Security** – JWT with short-lived access tokens + refresh rotation. All PII fields encrypted at rest.
4. **Developer Experience** – Every service has an OpenAPI 3.1 spec, a Dockerfile, and a health endpoint on `/actuator/health`.

## Target Users
| Persona | Pain Point | PayStream Solution |
|---|---|---|
| Finance Ops | Manual reconciliation errors | Immutable double-entry ledger with daily reports |
| Risk Analyst | Fraud spotted hours later | Sub-100ms real-time fraud detection via Kafka |
| End User | No visibility on transfer status | Live WebSocket feed on React dashboard |
| SRE / DevOps | Cascading failures | Circuit breakers + Grafana dashboards |

## Non-Negotiables
- No shared databases between microservices.
- All inter-service HTTP calls go through Spring Cloud Gateway.
- Kafka topics are the source of truth for all payment state transitions.
- The React frontend must look and feel like a Stripe dashboard — clean, data-dense, no amateur padding/spacing.
