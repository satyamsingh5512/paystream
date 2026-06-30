---
inclusion: always
---

# PayStream – Technology Stack & Constraints

## Backend
| Layer | Technology | Notes |
|---|---|---|
| Framework | Spring Boot 3.3.x | Java 21, virtual threads enabled |
| Security | Spring Security 6.3 | Stateless JWT; no sessions |
| Messaging | Apache Kafka 3.7 | 3 topics: `payment.events`, `fraud.alerts`, `notification.events` |
| Cache | Redis 7.2 | Lettuce client; rate limiting, fraud counters, session cache |
| Database | PostgreSQL 16 | One schema per service; Flyway migrations; optimistic locking |
| Service Discovery | Netflix Eureka | All services register on startup |
| Config | Spring Cloud Config Server | Git-backed; all services pull config on boot |
| Resilience | Resilience4j 2.x | Circuit breaker + retry + bulkhead on ALL RestClient calls |
| Observability | Micrometer + Prometheus + Grafana | Loki for log aggregation |
| API Docs | springdoc-openapi 2.x | OpenAPI 3.1 per service at `/swagger-ui.html` |
| Build | Maven 3.9 | Multi-module root POM; BOM for version alignment |
| Container | Docker + Docker Compose | One `docker-compose.yml` at root |

## Frontend
| Layer | Technology |
|---|---|
| Framework | React 18 + TypeScript 5 + Vite |
| Styling | Tailwind CSS 3.4 + shadcn/ui |
| State | Zustand |
| Data fetching | TanStack Query v5 |
| Charts | Recharts 2.x |
| Real-time | WebSocket API (STOMP over SockJS) |
| Forms | React Hook Form + Zod |
| Icons | Lucide React |
| Routing | React Router v6 |

## Code Style Rules
- **Java**: Google Java Style Guide. Records for DTOs. Sealed interfaces for domain events.
- **TypeScript**: Strict mode. Named exports only. No `any`.
- **CSS**: Utility-first (Tailwind). No inline styles. Design tokens only.
- **Commits**: Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`).
- **Tests**: JUnit 5 + Mockito for unit; Testcontainers for integration.
