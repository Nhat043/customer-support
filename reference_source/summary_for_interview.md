# Customer Support Hub - Interview Summary

## One-line pitch

`customer-support` is a Java Spring backend for a multi-tenant support desk where companies manage requests, teams, knowledge, notifications, attachments, and an AI assistant with tenant isolation.

## What the system does

- authenticates users with JWT and a refresh cookie
- isolates data by organization and workspace
- manages customer requests through a workflow
- supports comments and attachments on each request
- manages team members and roles
- supports a knowledge base so the AI can answer with citations
- prepares notification and assistant panels

## Core tech

- Java 17
- Spring Boot 3
- Spring Security
- Spring Data JPA
- Flyway
- PostgreSQL
- Docker and Docker Compose

## Important architecture ideas

### Auth

- access tokens are sent in the `Authorization` header
- refresh tokens are stored in an HTTP-only cookie

### Multi-tenant

- `organization` is the tenant root
- `workspace` is the tenant slice
- every business query must check org and workspace scope

### Workflow

- `workflow_items` are the main requests
- `workflow_events` store history and audit trails
- `comments` and `attachments` belong to a request

### Knowledge and AI

- Markdown documents can be uploaded
- documents are chunked
- chunks are indexed so the assistant can retrieve them
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

## API to remember

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
- `PATCH /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/comments/{commentId}`
- `DELETE /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/comments/{commentId}`
- `GET /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/attachments`
- `POST /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/attachments`
- `GET /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/attachments/{attachmentId}/download`
- `DELETE /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/attachments/{attachmentId}`

## Deployment summary

- local runs use Docker Compose
- PostgreSQL is the primary database
- attachments are stored on the local filesystem in development
- Flyway manages schema changes
- production should use a managed database, object storage, HTTPS, and a secrets manager

## Docs folder

- [`project_overview.md`](./project_overview.md)
- [`database_schema.md`](./database_schema.md)
- [`api_design.md`](./api_design.md)
- [`frontend_architecture.md`](./frontend_architecture.md)
- [`deployment.md`](./deployment.md)
