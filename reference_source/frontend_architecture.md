# Frontend Architecture

This document describes the frontend architecture that should be used for `customer-support` when it is paired with the current Java Spring backend.

## Important context

This repo does **not** currently contain frontend source code.
This document is a target architecture so that it can be used to:

- explain what the frontend needs to do
- map UI flows to backend APIs
- prepare interview notes
- serve as the blueprint for a future frontend scaffold

## 1. Frontend responsibilities

The Customer Support Hub frontend is not only responsible for rendering UI.
It must do five main things:

1. authenticate users
2. keep tenant context by organization and workspace
3. show requests, comments, attachments, team, knowledge, and notifications
4. open the assistant panel and call only approved backend tools
5. keep UI state in sync after create, update, and delete actions

## 2. Recommended stack

### Core stack

- **Next.js App Router**
- **TypeScript**
- **Tailwind CSS**
- **React Server Components + Client Components**
- **TanStack Query** for server state
- **Zustand** or React Context for small UI state
- **Fetch wrapper** or `ky`/`axios` for API access

### Why this stack

#### Next.js App Router

- clear route-based layout structure
- a good fit for dashboard-heavy apps
- easy to split `auth`, `org`, `workspace`, and `settings`
- works well with SSR and CSR together

#### TypeScript

- type-safe request and response handling
- fewer bugs when status, role, or payload shapes change

#### Tailwind CSS

- fast dashboard UI development
- easy theme consistency
- a good fit for component-based enterprise UI

#### TanStack Query

- caches server state
- refetches after mutations
- keeps dashboard, detail view, assistant, and notification badge in sync

#### Zustand / Context

- only for UI state such as:
  - sidebar open or closed
  - selected organization or workspace
  - assistant panel open or closed
  - active tab

Redux is not necessary unless the project becomes much more complex.

## 3. Frontend folder structure

If the frontend is implemented for real, the structure should look like this:

```text
apps/web
├── app
│   ├── login
│   ├── register
│   ├── forgot-password
│   ├── reset-password
│   ├── orgs
│   │   └── [orgSlug]
│   │       ├── layout.tsx
│   │       ├── page.tsx
│   │       ├── workflow-items
│   │       ├── knowledge
│   │       ├── team
│   │       ├── notifications
│   │       └── settings
│   └── api
├── components
│   ├── layout
│   ├── ui
│   ├── forms
│   ├── dashboard
│   ├── workflow
│   ├── comments
│   ├── attachments
│   ├── knowledge
│   ├── notifications
│   └── assistant
├── lib
│   ├── api-client.ts
│   ├── auth.ts
│   ├── query-client.ts
│   ├── tenant.ts
│   └── utils.ts
├── hooks
├── stores
├── types
└── styles
```

## 4. Page map

### Public pages

- `/login`
- `/register`
- `/forgot-password`
- `/reset-password`
- `/join?invitation=...`

### Authenticated organization pages

- `/orgs/[orgSlug]`  
  Overview dashboard
- `/orgs/[orgSlug]/workflow-items`  
  Request queue
- `/orgs/[orgSlug]/workflow-items/[workflowItemId]`  
  Request detail
- `/orgs/[orgSlug]/team`  
  Members and roles
- `/orgs/[orgSlug]/knowledge`  
  Knowledge base upload and source list
- `/orgs/[orgSlug]/notifications`  
  Notification inbox
- `/orgs/[orgSlug]/settings`  
  Profile, password, and workspace settings

## 5. Layout strategy

The frontend should use a nested layout structure:

1. **Public layout**
   - login, register, and landing pages
2. **Authenticated app shell**
   - top header
   - organization and workspace selector
   - main content
   - optional assistant drawer on the right
3. **Page-specific layout**
   - workflow list and detail
   - team
   - knowledge
   - settings

### Why this matters

- keeps header and navigation consistent
- allows the assistant drawer to stay open across pages
- prevents UI state from resetting on each navigation

## 6. Authentication architecture

### Token strategy

Frontend should treat auth like this:

- backend returns an access token in the response body
- backend sets a refresh token in an HTTP-only cookie
- frontend stores the access token in memory or other safe client storage
- frontend calls `/api/auth/refresh` when the token expires

### Practical flow

1. user logs in
2. frontend saves the access token
3. frontend calls authenticated APIs with the `Authorization` header
4. if a request returns `401`, frontend refreshes the token once
5. if refresh fails, redirect to `/login`

### UI states needed

- logged out
- logging in
- session expired
- authenticated
- role restricted

## 7. Tenant context architecture

The frontend must always know:

- active organization slug
- active workspace slug
- current user role
- current page type

### Tenant context source

- organization comes from the route: `/orgs/[orgSlug]`
- workspace usually comes from a selected dropdown or route query
- role comes from `/api/auth/me` plus organization membership data

### Why this matters

The UI can hide buttons, but the backend still enforces security.
Frontend tenant context is used for:

- request filtering
- routing
- UI labels
- permission-aware actions

## 8. Data fetching model

### Use TanStack Query for server state

Server state includes:

- organizations
- workspaces
- memberships
- workflow items
- comments
- attachments
- notifications
- knowledge documents
- assistant conversations

### Recommended patterns

- `useQuery` for list and detail loading
- `useMutation` for create, update, and delete
- invalidate related queries after mutation
- use optimistic updates only for simple UI actions

### Example invalidation logic

- create request -> invalidate request list, dashboard summary, and notification badge
- update request -> invalidate request detail, list, and events
- delete attachment -> invalidate request detail attachment list
- add member -> invalidate team page and organization detail

## 9. UI state model

Use a small store for UI-only concerns:

- assistant drawer open or closed
- active tab highlight
- selected workspace
- selected organization
- notification badge count
- mobile sidebar open or closed

Do **not** put everything in global state.
Database-backed data should stay in the query cache.

## 10. Assistant panel architecture

The AI assistant is not a separate page in production.
It should be a **right-side drawer** that can open from any authenticated page.

### Assistant UI responsibilities

- show conversation history
- show a "new chat" button
- show available tool hints
- show citations or sources
- show current run status

### Assistant behavior

- can create or update workflow items only through approved backend tools
- should never generate raw SQL
- should not bypass tenant scope
- should keep conversation state per user and per tenant

### Frontend implication

- assistant drawer state should persist across route changes
- if the drawer is open and the user navigates to another page, it should remain open
- navigation should not reset the conversation unless the user clicks "New chat"

## 11. Knowledge base page

The knowledge page is for workspace-owned support documents.

### Core actions

- upload markdown
- list source documents
- show indexed status
- show chunk or source preview
- retry failed indexing
- delete a document

### UX rule

- show file-level summary first
- hide chunk details behind a source preview or expand action
- do not overwhelm the user with raw chunks by default

### Data source

- future knowledge APIs
- backend schema already reserves:
  - `knowledge_documents`
  - `knowledge_chunks`

## 12. Workflow page architecture

### Pages

- list page
- detail page
- create form
- update form

### Key UI pieces

- status badge
- priority badge
- open request button
- comment thread
- attachment section
- event timeline

### Refresh strategy

After a mutation, the page should refetch or invalidate data:

- create request
- update request
- delete request
- add comment
- upload or delete attachment

This is important because the same request can be visible in:

- dashboard
- queue page
- detail page
- assistant source results

## 13. Notification architecture

The notifications tab should show event-driven updates such as:

- request created
- request status changed
- member invited
- attachment added
- knowledge indexed

### UX rule

- show a badge or `!` on the tab when unread notifications exist
- clear the badge when notifications are read

### Frontend data pattern

- query the unread count when the shell loads
- keep the badge in UI state
- refetch after mutation or on a periodic poll

## 14. Role-aware rendering

The frontend should render based on role:

- **Owner**
  - manage company settings
  - manage team
  - manage knowledge
  - full workflow control
- **Admin**
  - manage team
  - manage workflow
  - manage knowledge
- **Member**
  - create or update requests
  - comment
  - attach files
- **Viewer**
  - read-only

This is only UI behavior.
The backend must still enforce the same rules.

## 15. API client layer

Create one client wrapper so all pages use the same auth logic.

### Client responsibilities

- attach the access token
- include credentials for the refresh cookie
- normalize errors
- handle `401` with a single retry refresh
- parse structured error responses

### Why a wrapper matters

If every page calls `fetch` directly, auth refresh and error handling become inconsistent.

## 16. Suggested frontend component breakdown

### Layout components

- `AppHeader`
- `OrgWorkspaceSwitch`
- `RoleBadge`
- `UserMenu`
- `AssistantDrawer`

### Workflow components

- `WorkflowList`
- `WorkflowCard`
- `WorkflowStatusBadge`
- `WorkflowPriorityBadge`
- `WorkflowDetailPanel`
- `WorkflowEventTimeline`

### Team components

- `MemberList`
- `InviteMemberForm`
- `RoleSelect`

### Knowledge components

- `KnowledgeUploadCard`
- `KnowledgeDocumentList`
- `KnowledgeSourcePreview`
- `KnowledgeStatusBadge`

### Notification components

- `NotificationList`
- `NotificationBadge`

## 17. What should persist across routes

These should not reset during normal navigation:

- auth session
- active organization and workspace
- assistant drawer state
- query cache
- notification badge count

## 18. What should reset on route change

These can change per page:

- selected workflow item
- detail form state
- open modal
- search filter state
- local draft text

## 19. Interview explanation

If someone asks "what is the frontend architecture?", the clean answer is:

1. Next.js App Router provides route and layout structure.
2. Tailwind handles dashboard styling.
3. TanStack Query handles backend state and cache invalidation.
4. A small UI store handles shell state such as drawer and active workspace.
5. Auth uses an access token plus a refresh cookie.
6. Every authenticated page is tenant-aware through organization and workspace route context.
7. Assistant, workflow, knowledge, and notifications all share the same shell and tenant scope.

## 20. Current status

This frontend architecture is planned, not yet implemented in this JavaSpring repo.

The backend already supports the business model, so this document can be used as the blueprint for:

- building a real frontend later
- explaining the system in interviews
- keeping UI and API aligned

