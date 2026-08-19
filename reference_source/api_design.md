# API Design

This document describes the current API architecture for `customer-support`, where each route lives in the code, what the request and response shapes look like, and which tables each API works with.

## Source of truth

- Auth controller: [`src/main/java/com/nhat/workflowhub/auth/controller/AuthController.java`](../src/main/java/com/nhat/workflowhub/auth/controller/AuthController.java)
- Organization controller: [`src/main/java/com/nhat/workflowhub/organization/controller/OrganizationController.java`](../src/main/java/com/nhat/workflowhub/organization/controller/OrganizationController.java)
- Workspace controller: [`src/main/java/com/nhat/workflowhub/workspace/controller/WorkspaceController.java`](../src/main/java/com/nhat/workflowhub/workspace/controller/WorkspaceController.java)
- Membership controller: [`src/main/java/com/nhat/workflowhub/membership/controller/MembershipController.java`](../src/main/java/com/nhat/workflowhub/membership/controller/MembershipController.java)
- Workflow controller: [`src/main/java/com/nhat/workflowhub/workflow/controller/WorkflowController.java`](../src/main/java/com/nhat/workflowhub/workflow/controller/WorkflowController.java)
- Comment controller: [`src/main/java/com/nhat/workflowhub/comment/controller/CommentController.java`](../src/main/java/com/nhat/workflowhub/comment/controller/CommentController.java)
- Attachment controller: [`src/main/java/com/nhat/workflowhub/attachment/controller/AttachmentController.java`](../src/main/java/com/nhat/workflowhub/attachment/controller/AttachmentController.java)
- Notification controller shell: [`src/main/java/com/nhat/workflowhub/notification/controller/NotificationController.java`](../src/main/java/com/nhat/workflowhub/notification/controller/NotificationController.java)
- Knowledge controller shell: [`src/main/java/com/nhat/workflowhub/knowledge/controller/KnowledgeController.java`](../src/main/java/com/nhat/workflowhub/knowledge/controller/KnowledgeController.java)
- AI controller shell: [`src/main/java/com/nhat/workflowhub/ai/controller/AiController.java`](../src/main/java/com/nhat/workflowhub/ai/controller/AiController.java)

## API style

- Base path: `/api`
- Auth: JWT access token in `Authorization: Bearer <token>`
- Refresh token: HTTP-only cookie set by the backend
- Tenant isolation: almost every business route is under `/api/organizations/{orgSlug}/...`
- Payload style: JSON, except attachment upload which is `multipart/form-data`

## Request/response contract summary

The app follows a simple rule:

1. Frontend sends access token for API authorization.
2. Backend validates tenant scope from `orgSlug` and optional `workspaceSlug`.
3. Backend filters by organization/workspace before reading or mutating data.
4. Mutations return a response DTO and often a `Location` header for created resources.

## Auth flow

### `POST /api/auth/register`

**Code**

- [`AuthController.register(...)`](../src/main/java/com/nhat/workflowhub/auth/controller/AuthController.java)
- Request DTO: [`RegisterRequest.java`](../src/main/java/com/nhat/workflowhub/auth/dto/RegisterRequest.java)
- Response DTO: [`AuthResponse.java`](../src/main/java/com/nhat/workflowhub/auth/dto/AuthResponse.java)

**Request**

```json
{
  "email": "user@example.com",
  "password": "secret123",
  "fullName": "Nhat"
}
```

**Response**

- `200 OK`
- `Set-Cookie` with refresh token
- Body:

```json
{
  "accessToken": "jwt-access-token",
  "tokenType": "Bearer",
  "userId": "uuid",
  "email": "user@example.com",
  "fullName": "Nhat"
}
```

**Database**

- `users`
- `sessions`
- and organization/workspace bootstrap logic in the service layer

---

### `POST /api/auth/login`

**Code**

- [`AuthController.login(...)`](../src/main/java/com/nhat/workflowhub/auth/controller/AuthController.java)
- Request DTO: [`LoginRequest.java`](../src/main/java/com/nhat/workflowhub/auth/dto/LoginRequest.java)

**Request**

```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

**Response**

- `200 OK`
- `Set-Cookie` with refresh token
- Body is the same `AuthResponse`

**Notes**

- This is the main login endpoint used by the frontend.
- Passwords are validated against `users.password_hash`.

---

### `POST /api/auth/refresh`

**Code**

- [`AuthController.refresh(...)`](../src/main/java/com/nhat/workflowhub/auth/controller/AuthController.java)

**Behavior**

- Reads refresh token from the HTTP-only cookie
- Validates it against the session store
- Returns a new access token and rotates the refresh cookie

**Database**

- `sessions`

---

### `POST /api/auth/logout`

**Code**

- [`AuthController.logout(...)`](../src/main/java/com/nhat/workflowhub/auth/controller/AuthController.java)

**Behavior**

- Revokes the refresh session
- Clears the refresh cookie

**Database**

- `sessions`

---

### `GET /api/auth/me`

**Code**

- [`AuthController.me(...)`](../src/main/java/com/nhat/workflowhub/auth/controller/AuthController.java)

**Behavior**

- Returns the authenticated principal
- Useful for frontend header/profile rendering

---

## Organization API

### `GET /api/organizations`

**Code**

- [`OrganizationController.list(...)`](../src/main/java/com/nhat/workflowhub/organization/controller/OrganizationController.java)
- Response DTO: [`OrganizationResponse.java`](../src/main/java/com/nhat/workflowhub/organization/dto/OrganizationResponse.java)

**Purpose**

- List organizations that belong to the current user.

**Database**

- `organizations`
- `memberships`

---

### `POST /api/organizations`

**Code**

- [`OrganizationController.create(...)`](../src/main/java/com/nhat/workflowhub/organization/controller/OrganizationController.java)
- Request DTO: [`CreateOrganizationRequest.java`](../src/main/java/com/nhat/workflowhub/organization/dto/CreateOrganizationRequest.java)
- Response DTO: [`OrganizationDetailsResponse.java`](../src/main/java/com/nhat/workflowhub/organization/dto/OrganizationDetailsResponse.java)

**Request**

```json
{
  "name": "Nhat's Org",
  "slug": "nhat-s-org"
}
```

**Behavior**

- Creates the organization
- Creates the default workspace
- Creates owner membership
- Returns a created response and `Location` header

**Database**

- `organizations`
- `workspaces`
- `memberships`

---

### `GET /api/organizations/{slug}`

**Code**

- [`OrganizationController.get(...)`](../src/main/java/com/nhat/workflowhub/organization/controller/OrganizationController.java)

**Purpose**

- Fetch organization details by slug, including workspaces and memberships

**Response**

- [`OrganizationDetailsResponse.java`](../src/main/java/com/nhat/workflowhub/organization/dto/OrganizationDetailsResponse.java)

---

## Workspace API

### `GET /api/organizations/{orgSlug}/workspaces`

**Code**

- [`WorkspaceController.list(...)`](../src/main/java/com/nhat/workflowhub/workspace/controller/WorkspaceController.java)
- Response DTO: [`WorkspaceResponse.java`](../src/main/java/com/nhat/workflowhub/workspace/dto/WorkspaceResponse.java)

**Purpose**

- List all workspaces in one organization

**Database**

- `workspaces`

---

### `POST /api/organizations/{orgSlug}/workspaces`

**Code**

- [`WorkspaceController.create(...)`](../src/main/java/com/nhat/workflowhub/workspace/controller/WorkspaceController.java)
- Request DTO: [`CreateWorkspaceRequest.java`](../src/main/java/com/nhat/workflowhub/workspace/dto/CreateWorkspaceRequest.java)

**Request**

```json
{
  "name": "General",
  "slug": "general"
}
```

**Behavior**

- Creates a workspace under the organization
- Returns `201 Created` with the new workspace `Location`

**Database**

- `workspaces`

---

## Membership API

### `GET /api/organizations/{orgSlug}/memberships`

**Code**

- [`MembershipController.list(...)`](../src/main/java/com/nhat/workflowhub/membership/controller/MembershipController.java)
- Response DTO: [`MembershipResponse.java`](../src/main/java/com/nhat/workflowhub/membership/dto/MembershipResponse.java)

**Purpose**

- List team members in an organization / workspace scope

**Database**

- `memberships`
- `users`

---

### `POST /api/organizations/{orgSlug}/memberships`

**Code**

- [`MembershipController.add(...)`](../src/main/java/com/nhat/workflowhub/membership/controller/MembershipController.java)
- Request DTO: [`UpsertMembershipRequest.java`](../src/main/java/com/nhat/workflowhub/membership/dto/UpsertMembershipRequest.java)

**Request**

```json
{
  "email": "teammate@example.com",
  "role": "MEMBER",
  "workspaceSlug": "general"
}
```

**Behavior**

- Adds/invites a user into the organization or workspace scope
- Role is one of `OWNER`, `ADMIN`, `MEMBER`, `VIEWER`

**Database**

- `memberships`
- `users`

---

### `PATCH /api/organizations/{orgSlug}/memberships/{membershipId}/role/{role}`

**Code**

- [`MembershipController.updateRole(...)`](../src/main/java/com/nhat/workflowhub/membership/controller/MembershipController.java)

**Purpose**

- Change a member role in the org

---

### `DELETE /api/organizations/{orgSlug}/memberships/{membershipId}`

**Code**

- [`MembershipController.remove(...)`](../src/main/java/com/nhat/workflowhub/membership/controller/MembershipController.java)

**Purpose**

- Remove member access from the organization / workspace

---

## Workflow API

### `GET /api/organizations/{orgSlug}/workflow-items`

**Code**

- [`WorkflowController.list(...)`](../src/main/java/com/nhat/workflowhub/workflow/controller/WorkflowController.java)
- Response DTO: [`WorkflowItemResponse.java`](../src/main/java/com/nhat/workflowhub/workflow/dto/WorkflowItemResponse.java)

**Query params**

- `workspaceSlug` optional

**Purpose**

- List support requests in the selected organization and optionally one workspace

**Database**

- `workflow_items`

---

### `GET /api/organizations/{orgSlug}/workflow-items/{workflowItemId}`

**Code**

- [`WorkflowController.get(...)`](../src/main/java/com/nhat/workflowhub/workflow/controller/WorkflowController.java)

**Purpose**

- Fetch one request detail

**Database**

- `workflow_items`

---

### `POST /api/organizations/{orgSlug}/workflow-items`

**Code**

- [`WorkflowController.create(...)`](../src/main/java/com/nhat/workflowhub/workflow/controller/WorkflowController.java)
- Request DTO: [`CreateWorkflowItemRequest.java`](../src/main/java/com/nhat/workflowhub/workflow/dto/CreateWorkflowItemRequest.java)

**Request**

```json
{
  "title": "Payment is not working",
  "description": "Customer says card payment failed.",
  "status": "NEW",
  "priority": "MEDIUM",
  "workspaceSlug": "general",
  "assigneeUserId": null
}
```

**Behavior**

- Creates a new support request
- Defaults:
  - `status = NEW`
  - `priority = MEDIUM`

**Database**

- `workflow_items`
- `workflow_events`

---

### `PATCH /api/organizations/{orgSlug}/workflow-items/{workflowItemId}`

**Code**

- [`WorkflowController.update(...)`](../src/main/java/com/nhat/workflowhub/workflow/controller/WorkflowController.java)
- Request DTO: [`UpdateWorkflowItemRequest.java`](../src/main/java/com/nhat/workflowhub/workflow/dto/UpdateWorkflowItemRequest.java)

**Purpose**

- Update request title, description, status, priority, assignee, or due date

**Database**

- `workflow_items`
- `workflow_events`

---

### `DELETE /api/organizations/{orgSlug}/workflow-items/{workflowItemId}`

**Code**

- [`WorkflowController.delete(...)`](../src/main/java/com/nhat/workflowhub/workflow/controller/WorkflowController.java)

**Purpose**

- Delete a request

**Database**

- `workflow_items`
- cascade to comments, attachments, workflow events

---

### `GET /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/events`

**Code**

- [`WorkflowController.listEvents(...)`](../src/main/java/com/nhat/workflowhub/workflow/controller/WorkflowController.java)
- Response DTO: [`WorkflowEventResponse.java`](../src/main/java/com/nhat/workflowhub/workflow/dto/WorkflowEventResponse.java)

**Purpose**

- Read audit trail for request changes

**Database**

- `workflow_events`

---

## Comment API

### `GET /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/comments`

**Code**

- [`CommentController.list(...)`](../src/main/java/com/nhat/workflowhub/comment/controller/CommentController.java)
- Response DTO: [`CommentResponse.java`](../src/main/java/com/nhat/workflowhub/comment/dto/CommentResponse.java)

**Purpose**

- List all comments on one request

**Database**

- `comments`

---

### `POST /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/comments`

**Code**

- [`CommentController.add(...)`](../src/main/java/com/nhat/workflowhub/comment/controller/CommentController.java)
- Request DTO: [`CreateCommentRequest.java`](../src/main/java/com/nhat/workflowhub/comment/dto/CreateCommentRequest.java)

**Request**

```json
{
  "body": "I checked with the courier and the package is delayed."
}
```

**Database**

- `comments`
- `workflow_events` if the service records an event

---

### `PATCH /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/comments/{commentId}`

**Code**

- [`CommentController.update(...)`](../src/main/java/com/nhat/workflowhub/comment/controller/CommentController.java)
- Request DTO: [`UpdateCommentRequest.java`](../src/main/java/com/nhat/workflowhub/comment/dto/UpdateCommentRequest.java)

---

### `DELETE /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/comments/{commentId}`

**Code**

- [`CommentController.delete(...)`](../src/main/java/com/nhat/workflowhub/comment/controller/CommentController.java)

---

## Attachment API

### `GET /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/attachments`

**Code**

- [`AttachmentController.list(...)`](../src/main/java/com/nhat/workflowhub/attachment/controller/AttachmentController.java)
- Response DTO: [`AttachmentResponse.java`](../src/main/java/com/nhat/workflowhub/attachment/dto/AttachmentResponse.java)

**Purpose**

- List file metadata for one request

**Database**

- `attachments`

---

### `POST /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/attachments`

**Code**

- [`AttachmentController.upload(...)`](../src/main/java/com/nhat/workflowhub/attachment/controller/AttachmentController.java)

**Content type**

- `multipart/form-data`

**Form fields**

- `file`

**Behavior**

- Stores file bytes in the attachment storage layer
- Stores metadata in the DB

**Database**

- `attachments`

---

### `GET /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/attachments/{attachmentId}/download`

**Code**

- [`AttachmentController.download(...)`](../src/main/java/com/nhat/workflowhub/attachment/controller/AttachmentController.java)

**Behavior**

- Returns the binary resource
- Sets `Content-Disposition` so browser downloads the file

---

### `DELETE /api/organizations/{orgSlug}/workflow-items/{workflowItemId}/attachments/{attachmentId}`

**Code**

- [`AttachmentController.delete(...)`](../src/main/java/com/nhat/workflowhub/attachment/controller/AttachmentController.java)

**Purpose**

- Soft-delete attachment metadata and remove/mark the stored file

---

## Current placeholder APIs

These controllers exist but are not yet fully implemented:

- `/api/notifications`
- `/api/knowledge`
- `/api/ai`

That means the frontend can already plan for them, but the business logic is still a future step.

## Folder structure for API code

The feature code is organized like this:

```text
src/main/java/com/nhat/workflowhub
├── auth
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   └── service
├── organization
├── workspace
├── membership
├── workflow
├── comment
├── attachment
├── notification
├── knowledge
└── ai
```

## How to explain this in an interview

When someone asks “how does the API work?”, the short answer is:

1. Auth issues JWT access token + refresh cookie.
2. Organization is the tenant root.
3. Workspace is the second tenant level.
4. Workflow item is the support request entity.
5. Comments and attachments are nested under workflow items.
6. Membership controls who can see and mutate each scope.
7. Knowledge and AI are reserved in schema and controller shells for the next phase.

## Next docs to add

- `reference_source/business_flow_and_api.md` if you want one combined story
- `reference_source/frontend_architecture.md` if you want FE behavior / routing / state
- `reference_source/deployment.md` if you want Docker / GCP / Terraform explanation
