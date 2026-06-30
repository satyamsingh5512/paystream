---
inclusion: always
---

# PayStream – Project Structure

```
paystream/
├── docker-compose.yml
├── pom.xml
├── .env.example
├── config-server/          (port 8888)
├── api-gateway/            (port 8080)
├── auth-service/           (port 8081)
├── account-service/        (port 8082)
├── transaction-service/    (port 8083)
├── fraud-service/          (port 8084)
├── notification-service/   (port 8085)
├── ledger-service/         (port 8086)
├── frontend/               (port 5173 dev / 80 prod)
│   └── src/
│       ├── components/
│       │   ├── ui/
│       │   ├── dashboard/
│       │   ├── transactions/
│       │   ├── fraud/
│       │   └── charts/
│       ├── pages/
│       ├── hooks/
│       ├── stores/
│       ├── services/
│       └── types/
└── docs/
    ├── architecture.md
    ├── api-contracts/
    └── adr/
```

## Naming Conventions
- Java packages: `com.paystream.<service-name>.<layer>`
- Kafka topics: `kebab-case` (e.g., `payment-initiated`, `fraud-alert-raised`)
- REST endpoints: `kebab-case` nouns (e.g., `/api/v1/transactions`)
- React components: `PascalCase.tsx`
- Zustand stores: `use<Name>Store.ts`
- Environment variables: `SCREAMING_SNAKE_CASE`
