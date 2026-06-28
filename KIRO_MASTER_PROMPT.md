# PayStream — Kiro Master Prompt

Paste this entire prompt into Kiro chat as your FIRST message.

---

You are building **PayStream**, a production-grade distributed payment processing platform.
This is being built to the standard of a senior engineering team at a Tier-1 fintech company.
The full specification, architecture, design system, and task breakdown are in the `.kiro/` directory.

## Context Files (already in place)
- `.kiro/steering/product.md` — product vision, non-negotiables
- `.kiro/steering/tech.md` — full technology stack and code style rules
- `.kiro/steering/structure.md` — project layout, naming conventions, port assignments
- `.kiro/steering/ui-design-system.md` — Stripe-quality dark mode design system
- `.kiro/specs/paystream/requirements.md` — EARS-format requirements (8 functional areas)
- `.kiro/specs/paystream/design.md` — service architecture, Kafka topology, data flow
- `.kiro/specs/paystream/tasks.md` — 70+ atomic tasks across 10 phases

## Agents Available
- `@senior-java-architect` — Spring Boot, Kafka, Saga, Resilience4j, Testcontainers
- `@senior-react-engineer` — React components, Zustand, TanStack Query, WebSocket
- `@devops-engineer` — Dockerfile, docker-compose, Prometheus, Grafana, GitHub Actions

## Skills Available
- `/java-microservice` — scaffold a new Spring Boot service
- `/react-fintech-ui` — build a React component per design system
- `/distributed-patterns` — implement Saga, CQRS, Outbox, Circuit Breaker
- `/observability` — add Prometheus metrics, JSON logging, Grafana dashboards

## Hooks Active (automatic)
- Java Test on Save — runs mvn test when any .java is saved
- OpenAPI Sync — validates @Operation annotations on Controller saves
- UI Design Check — validates design tokens on .tsx saves
- Migration Safety — checks Flyway SQL for destructive operations
- Post-Task Tests — runs tests after each spec task
- Commit Message Lint — generates Conventional Commit on agent stop

---

## Start Command

Begin execution of the spec tasks in `.kiro/specs/paystream/tasks.md`.

Follow this build order — each phase must be complete and tested before the next:

**Phase 1 first**: Infrastructure Foundation — root pom.xml, docker-compose.yml, config-server, api-gateway, Eureka.

Rules:
1. Every task = complete implementation. No stubs. No TODOs.
2. Write the test immediately after each implementation.
3. Verify acceptance criteria from requirements.md.
4. Mark the checkbox in tasks.md as complete `[x]`.
5. Use `@senior-java-architect` for all Java work.
6. Use `@devops-engineer` for all Docker/infra work.

After Phase 1 completes, PAUSE and show:
- List of all files created
- Output of `docker compose config` (validate compose file)
- Any open questions before Phase 2

**Quality bar**: Every file must be PR-ready for a senior engineering team.
