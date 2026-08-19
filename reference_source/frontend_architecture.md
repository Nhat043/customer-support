# Frontend Architecture

This document describes the frontend architecture that should be used for `customer-support` when it is paired with the current JavaSpring backend.

## Important context

The current repository **does not include frontend source code yet**, so this is the target architecture used to:

- explain what the frontend must do
- map UI flows to backend APIs
- serve as interview notes
- provide a blueprint for a future frontend scaffold

## 1. Frontend responsibilities

The Customer Support Hub frontend is not just a UI renderer.
It must handle five core responsibilities:

1. authenticate the user
2. keep tenant context by organization and workspace
3. display requests, comments, attachments, team data, knowledge, and notifications
4. open the assistant panel and call backend-approved tools
5. keep UI state in sync after create, update, and delete actions

## 2. Recommended stack

### Core stack

- **Next.js App Router**
- **TypeScript**
- **Tailwind CSS**
- **React Server Components + Client Components**
- **TanStack Query** for server state
- **Zustand** or React Context for small UI state
- **Fetch wrapper** or `ky` / `axios` for the API client

### Why this stack

#### Next.js App Router

- clear route-based layout
- works well for multi-page dashboards
- easy to split `auth`, `org`, `workspace`, and `settings`
- fits a mixed SSR / CSR model

#### TypeScript

- type-safe request and response handling from backend DTOs
- fewer bugs when status, role, or payload shapes change

#### Tailwind CSS

- fast dashboard UI development
- easy to keep the theme consistent
- well suited for component-based enterprise UI

#### TanStack Query

- server-data caching
- refetch after mutations
- keeps dashboard, detail pages, assistant, and notification badges in sync

#### Zustand / Context

- only for UI state such as:
  - sidebar open/close
  - selected organization/workspace
  - assistant panel open/close
  - active tab

Do **not** use Redux for everything if the project is not complex enough yet.

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
   - login / register / landing pages
2. **Authenticated app shell**
   - top header
   - organization and workspace selector
   - left or center content area
   - optional assistant drawer on the right
3. **Page-specific layout**
   - workflow list and detail
   - team
   - knowledge
   - settings

### Why this matters

- keeps the header and sidebar consistent
- allows the assistant drawer to persist across pages
- prevents the UI from resetting on every navigation

## 6. Authentication architecture

### Token strategy

The frontend should treat auth like this:

- the backend returns the access token in the response body
- the backend sets the refresh token in an HTTP-only cookie
- the frontend stores the access token in memory or safe client storage
- the frontend calls `/api/auth/refresh` when the token expires

### Practical flow

1. The user logs in.
2. The frontend stores the access token.
3. The frontend calls authenticated APIs with the `Authorization` header.
4. If a request returns `401`, the frontend refreshes the token once.
5. If refresh fails, redirect to `/login`.

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
- workspace usually comes from a dropdown or a route query
- role comes from `/api/auth/me` and organization membership data

### Why this matters

The UI can hide buttons, but the backend must still enforce security.
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
- `useMutation` for create, update, and delete operations
- invalidate related queries after mutation
- use optimistic updates only for simple UI actions

### Example invalidation logic

- create request -> invalidate request list, dashboard summary, and notification badge
- update request -> invalidate request detail, list, and events
- delete attachment -> invalidate the request detail attachment list
- add member -> invalidate the team page and organization detail

## 9. UI state model

Use a small store for UI-only concerns:

- assistant drawer open/close
- active tab highlight
- selected workspace
- selected organization
- notification badge count
- mobile sidebar open/close

Do **not** put everything in global state.
Database-backed data should stay in the query cache.

## 10. Assistant panel architecture

The AI assistant is not a separate app page in production.
It should be a **right-side drawer** that can open from any authenticated page.

### Assistant UI roles

- show conversation history
- show a "new chat" button
- show available tool hints
- show citations and sources
- show current run status

### Assistant behavior

- can create or update workflow items only through approved backend tools
- should never generate raw SQL
- should not bypass tenant scope
- should keep conversations per user and per tenant

### Frontend implication

- the assistant drawer state should persist across route changes
- if the drawer is open and the user navigates to another page, it should remain open
- navigation should not reset the conversation unless the user clicks "New chat"

## 11. Knowledge base page

The knowledge page is for workspace-owned support documents.

### Core actions

- upload Markdown
- list source documents
- show indexed status
- show chunk or source previews
- retry failed indexing
- delete a document

### UX rule

- show a file-level summary first
- hide chunk details behind source previews or expand actions
- do not overwhelm the user with raw chunks by default

### Data source

- future `GET` and `POST` knowledge APIs
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

After a mutation, the page should refetch data or invalidate the query:

- create request
- update request
- delete request
- add comment
- upload or delete attachment

This matters because the same request can appear in:

- the dashboard
- the queue page
- the detail page
- assistant source results

## 13. Notification architecture

The notification tab should show event-driven updates such as:

- request created
- request status changed
- member invited
- attachment added
- knowledge indexed

### UX rule

- show a badge or `!` on the tab when unread notifications exist
- clear the badge when notifications are read

### Frontend data pattern

- query the unread count on shell load
- keep the badge in the UI store
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
  - create and update requests
  - comment
  - attach files
- **Viewer**
  - read-only

### Important

This is only UI behavior.
The backend must still enforce the same rules.

## 15. API client layer

Create a single client wrapper so all pages use the same auth logic.

### Client responsibilities

- attach the access token
- include credentials for the refresh cookie
- normalize errors
- handle `401` with one retry refresh
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
- active query cache
- notification badge count

## 18. What should reset on route change

These can change per page:

- selected workflow item
- detail form state
- open modal
- search filter state
- local draft text

## 19. Interview explanation

If someone asks, "what is the frontend architecture?", the clean answer is:

1. Next.js App Router provides route and layout structure.
2. Tailwind handles dashboard styling.
3. TanStack Query handles backend state and cache invalidation.
4. A small UI store handles shell state such as the drawer and active workspace.
5. Auth uses access token plus refresh cookie.
6. Every authenticated page is tenant-aware through organization and workspace route context.
7. Assistant, workflow, knowledge, and notifications all share the same shell and tenant scope.

## 20. Current status

This frontend architecture is **planned**, not yet implemented in this JavaSpring repository.

The backend already supports the business model, so this document can be used as the blueprint for:

- building a real frontend later
- explaining the system in interviews
- keeping the UI and API aligned
