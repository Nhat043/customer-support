# Customer Support Hub - Java Spring

This is the Java Spring version of the same customer support workflow domain used by the Next.js project.

## What It Will Demonstrate

- secure authentication
- multi-tenant workspace isolation
- request / comment / attachment workflow
- notifications and realtime sync
- AI tool-calling assistant
- knowledge base retrieval
- observability and deployment readiness

## How To Run Later

```bash
mvn spring-boot:run
```

## Docker Local

1. copy `.env.example` to `.env`
2. run `docker compose up --build`

Run those commands from `javaspring/customer-support/`.

The local compose stack uses PostgreSQL with database name `customer_support_hub_local`.

## Structure

- `src/main/java/com/nhat/workflowhub`
- feature-first packages
- Flyway migrations under `src/main/resources/db/migration`
