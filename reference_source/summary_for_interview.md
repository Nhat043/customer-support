# Customer Support Hub - Interview Summary

## One-line pitch

`customer-support` is a Java Spring backend for a multi-tenant support desk where a company manages requests, team members, knowledge, notifications, attachments, and an AI assistant with tenant isolation.

## What the system does

- authenticates users with JWT plus a refresh cookie
- separates data by organization and workspace
- manages customer requests through a workflow
- supports comments and attachments on each request
- manages team members and roles
- supports a knowledge base so AI can answer with citations
- prepares notification and assistant panels

## Core tech

- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- Flyway
- PostgreSQL
- Docker and Docker Compose

## Important architecture ideas

### Auth

- access token goes in the `Authorization` header
- refresh token goes in an HTTP-only cookie

### Multi-tenant

- `organization` is the tenant root
- `workspace` is the tenant slice
- every business query must check organization and workspace scope

### Workflow

- `workflow_items` is the main request table
- `workflow_events` stores history and audit trail
- `comments` and `attachments` belong to each request

### Knowledge and AI

- markdown documents are uploaded
- documents are chunked
- chunks are indexed for retrieval
- the assistant can only use approved tools

## Tables to remember

- `users`
- `sessions`
- `organizations`
- `workspaces`
- `memberships`
- `workflow_items`
- `workflow_events`
- `comments`
- `attachments`
- `notifications`
- `knowledge_documents`
- `knowledge_chunks`
- `agent_conversations`
- `agent_messages`
- `agent_memory_items`

## APIs to remember

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `GET /api/organizations`
- `POST /api/organizations`
- `GET /api/organizations/{orgSlug}/workspaces`
- `POST /api/organizations/{orgSlug}/workspaces`
- `GET /api/organizations/{orgSlug}/workflow-items`
- `POST /api/organizations/{orgSlug}/workflow-items`
- `PATCH /api/organizations/{orgSlug}/workflow-items/{workflowItemId}`
- `DELETE /api/organizations/{orgSlug}/workflow-items/{workflowItemId}`
- `GET /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/comments`
- `POST /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/comments`
- `GET /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/attachments`
- `POST /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/attachments`

## Deployment summary

- local runs with Docker Compose
- PostgreSQL is the main database
- attachments are stored on the local filesystem in development
- Flyway manages schema migrations
- production should use managed DB, object storage, HTTPS, and a secrets manager

## Folder docs

- [`project_overview.md`](./project_overview.md)
- [`database_schema.md`](./database_schema.md)
- [`api_design.md`](./api_design.md)
- [`frontend_architecture.md`](./frontend_architecture.md)
- [`deployment.md`](./deployment.md)

