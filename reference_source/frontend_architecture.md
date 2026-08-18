# Frontend Architecture

Tài liệu này mô tả kiến trúc frontend nên dùng cho `customer-support` khi ghép với backend JavaSpring hiện tại.

## Important context

Repo hiện tại **chưa chứa frontend source code**, nên đây là tài liệu kiến trúc mục tiêu để:

- giải thích frontend phải làm gì
- map rõ UI flow với backend API
- dùng làm note phỏng vấn
- làm nền để sau này scaffold frontend thật

## 1. Frontend responsibilities

Frontend của Customer Support Hub không chỉ là “render UI”.
Nó phải làm 5 việc chính:

1. xác thực người dùng
2. giữ tenant context theo organization/workspace
3. hiển thị request / comment / attachment / team / knowledge / notification
4. mở assistant panel và gọi các tool do backend cho phép
5. đồng bộ trạng thái UI sau khi tạo, cập nhật, xóa dữ liệu

## 2. Recommended stack

### Core stack

- **Next.js App Router**
- **TypeScript**
- **Tailwind CSS**
- **React Server Components + Client Components**
- **TanStack Query** cho server state
- **Zustand** hoặc React Context cho UI state nhỏ
- **Fetch wrapper** hoặc `ky`/`axios` cho API client

### Why this stack

#### Next.js App Router

- route-based layout rõ ràng
- hợp với dashboard nhiều page
- dễ chia `auth`, `org`, `workspace`, `settings`
- phù hợp SSR/CSR mix

#### TypeScript

- type-safe request/response từ backend DTO
- giảm lỗi khi đổi status, role, payload

#### Tailwind CSS

- build UI dashboard nhanh
- dễ tạo theme nhất quán
- phù hợp component-based enterprise UI

#### TanStack Query

- cache server data
- refetch sau mutation
- đồng bộ dashboard / detail / assistant / notification badge

#### Zustand / Context

- chỉ dùng cho UI state:
  - sidebar open/close
  - selected organization/workspace
  - assistant panel open/close
  - active tab

Không nên dùng Redux cho mọi thứ nếu project chưa thật sự phức tạp.

## 3. Frontend folder structure

Nếu làm frontend thật, cấu trúc nên kiểu này:

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
  Knowledge base upload / source list
- `/orgs/[orgSlug]/notifications`  
  Notification inbox
- `/orgs/[orgSlug]/settings`  
  Profile / password / workspace settings

## 5. Layout strategy

The frontend should use a nested layout structure:

1. **Public layout**
   - login/register/landing
2. **Authenticated app shell**
   - top header
   - org/workspace selector
   - left/center content
   - optional assistant drawer on the right
3. **Page-specific layout**
   - workflow list/detail
   - team
   - knowledge
   - settings

### Why this matters

- keeps header/sidebar consistent
- assistant drawer can persist across pages
- UI state does not reset on every navigation

## 6. Authentication architecture

### Token strategy

Frontend should treat auth like this:

- backend returns access token in response body
- backend sets refresh token in HTTP-only cookie
- frontend stores access token in memory or safe client storage
- frontend calls `/api/auth/refresh` when token expires

### Practical flow

1. user logs in
2. frontend saves access token
3. frontend calls authenticated APIs with `Authorization` header
4. if request returns `401`, frontend refreshes token once
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

- organization comes from route: `/orgs/[orgSlug]`
- workspace usually comes from selected dropdown or route query
- role comes from `/api/auth/me` + organization/membership data

### Why this matters

The UI can hide buttons, but backend still enforces security.
Frontend tenant context is for:

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

- `useQuery` for list/detail loading
- `useMutation` for create/update/delete
- invalidate related queries after mutation
- optimistic update only for simple UI actions

### Example invalidation logic

- create request -> invalidate request list + dashboard summary + notification badge
- update request -> invalidate request detail + list + events
- delete attachment -> invalidate request detail attachment list
- add member -> invalidate team page + org detail

## 9. UI state model

Use a small store for UI-only concerns:

- assistant drawer open/close
- active tab highlight
- selected workspace
- selected organization
- notification badge count
- mobile sidebar open/close

Do **not** put everything in global state.
Database-backed data should stay in query cache.

## 10. Assistant panel architecture

The AI assistant is not a separate app page in production.
It should be a **right-side drawer** that can open from any authenticated page.

### Assistant UI roles

- show conversation history
- show “new chat” button
- show available tool hints
- show citations / sources
- show current run status

### Assistant behavior

- can create/update workflow items only through approved backend tools
- should never generate raw SQL
- should not bypass tenant scope
- should keep conversation per user and per tenant

### Frontend implication

- assistant drawer state should persist across route changes
- if drawer is open and user navigates to another page, it should remain open
- navigation should not reset the conversation unless user clicks “New chat”

## 11. Knowledge base page

The knowledge page is for workspace-owned support docs.

### Core actions

- upload markdown
- list source documents
- show indexed status
- show chunks/source preview
- retry failed indexing
- delete a document

### UX rule

- show file-level summary first
- hide chunk details behind source preview / expand
- do not overwhelm user with raw chunks by default

### Data source

- `GET/POST` future knowledge APIs
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

After mutation, page should refetch data or invalidate query:

- create request
- update request
- delete request
- add comment
- upload/delete attachment

This is important because the same request can be visible in:

- dashboard
- queue page
- detail page
- assistant source result

## 13. Notification architecture

The notification tab should show event-driven updates like:

- request created
- request status changed
- member invited
- attachment added
- knowledge indexed

### UX rule

- show badge or `!` on the tab when unread notifications exist
- clear badge when notifications are read

### Frontend data pattern

- query unread count on shell load
- keep badge in UI store
- refetch after mutation or periodic poll

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
  - create/update requests
  - comment
  - attach files
- **Viewer**
  - read-only

### Important

This is only UI behavior.
Backend must still enforce the same rules.

## 15. API client layer

Create a single client wrapper so all pages use the same auth logic.

### Client responsibilities

- attach access token
- include credentials for refresh cookie
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
- active organization/workspace
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

If someone asks “what is the frontend architecture?”, the clean answer is:

1. Next.js App Router provides route and layout structure.
2. Tailwind handles dashboard UI styling.
3. TanStack Query handles backend state and cache invalidation.
4. Small UI store handles shell state like drawer and active workspace.
5. Auth uses access token + refresh cookie.
6. Every authenticated page is tenant-aware through organization/workspace route context.
7. Assistant, workflow, knowledge, and notifications all share the same shell and tenant scope.

## 20. Current status

This frontend architecture is **planned**, not yet implemented in this JavaSpring repo.

The backend already supports the business model, so this document can be used as the blueprint for:

- building a real frontend later
- explaining the system in interviews
- making sure UI and API stay aligned

