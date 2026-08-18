# Customer Support Hub - Interview Summary

## One-line pitch

`customer-support` là backend Java Spring cho một support desk đa tenant, nơi company quản lý request, team, knowledge, notifications, attachments, và AI assistant có tenant isolation.

## What the system does

- xác thực user bằng JWT + refresh cookie
- tách dữ liệu theo organization / workspace
- quản lý customer requests theo workflow
- hỗ trợ comments và attachments theo request
- quản lý team member / role
- hỗ trợ knowledge base để AI trả lời có citation
- chuẩn bị notification và assistant panel

## Core tech

- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- Flyway
- PostgreSQL
- Docker / Docker Compose

## Important architecture ideas

### Auth

- access token đi trong `Authorization`
- refresh token đi trong HTTP-only cookie

### Multi-tenant

- `organization` là tenant root
- `workspace` là tenant slice
- mọi query business đều phải check org/workspace scope

### Workflow

- `workflow_items` là request chính
- `workflow_events` lưu history/audit trail
- `comments` và `attachments` gắn vào request

### Knowledge / AI

- markdown document được upload
- document được chunk
- chunk được index để assistant truy hồi
- assistant chỉ được dùng tool đã cho phép

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
- `GET/POST /api/organizations`
- `GET/POST /api/organizations/{orgSlug}/workspaces`
- `GET/POST/PATCH/DELETE /api/organizations/{orgSlug}/workflow-items`
- `GET/POST/PATCH/DELETE /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/comments`
- `GET/POST/DELETE /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/attachments`

## Deployment summary

- local run bằng Docker Compose
- PostgreSQL là DB chính
- attachments lưu local filesystem trong dev
- Flyway quản lý schema
- production nên dùng managed DB + object storage + HTTPS + secrets manager

## Folder docs

- [`project_overview.md`](./project_overview.md)
- [`database_schema.md`](./database_schema.md)
- [`api_design.md`](./api_design.md)
- [`frontend_architecture.md`](./frontend_architecture.md)
- [`deployment.md`](./deployment.md)

