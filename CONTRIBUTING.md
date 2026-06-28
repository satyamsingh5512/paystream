# Contributing to PayStream

Thanks for contributing! This guide keeps the codebase consistent and PR-ready.

## Branching

- Branch from `main`. Never push directly to `main`.
- Name branches `<type>/<short-description>`, e.g. `feat/ledger-export`, `fix/saga-timeout`.
- Types follow Conventional Commits: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`.

## Commits

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <imperative description>
```

Examples:
- `feat(transaction-service): add idempotency key to saga start`
- `fix(account-service): retry on optimistic lock during credit`
- `docs(readme): add reconciliation section`

Scope is the module/service or `frontend`.

## Code style

- **Java:** Google Java Style. Records for DTOs and events; sealed/enums for domain types.
  Every public method has Javadoc. Typed exceptions with context in messages. No raw types.
- **TypeScript:** strict mode, named exports, no `any`. Props interfaces above components.
- **CSS:** Tailwind utilities only — use design tokens, never hardcoded hex.
- **Tests:** JUnit 5 + Mockito for unit; Testcontainers for integration. Vitest + RTL for frontend.

## Before opening a PR

1. `./mvnw -B verify` passes (build + unit + integration tests).
2. `cd frontend && npm run lint && npm run test -- --run && npm run build` passes.
3. New behaviour is covered by tests.
4. Controllers carry OpenAPI annotations; new tables ship with Flyway migrations.
5. `docker compose config` still validates if you touched infra.

## PR template

Each PR description should include:

- **Summary** — what changed and why.
- **Testing** — what you ran and what passed.
- **Risk / rollout** — migrations, config, or breaking changes; how to roll back.

## Code review checklist

- [ ] Single responsibility; no dead code or stray TODOs.
- [ ] Transaction boundaries (`@Transactional`) are correct; no remote calls holding long DB locks unnecessarily.
- [ ] Kafka consumers are idempotent (dedupe on `messageId`).
- [ ] No secrets committed; `.env` stays untracked.
- [ ] Thread-safety on shared mutable state; null checks on external inputs.
- [ ] Accessibility: labels, `aria-label` on icon buttons, focus-visible rings (frontend).
