create table users (
  id uuid primary key,
  email varchar(255) not null unique,
  password_hash varchar(255) not null,
  full_name varchar(255) not null,
  status varchar(32) not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);

create table organizations (
  id uuid primary key,
  name varchar(255) not null,
  slug varchar(255) not null unique,
  owner_user_id uuid not null references users(id),
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);

create table workspaces (
  id uuid primary key,
  organization_id uuid not null references organizations(id) on delete cascade,
  name varchar(255) not null,
  slug varchar(255) not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  unique (organization_id, slug)
);

create table memberships (
  id uuid primary key,
  organization_id uuid not null references organizations(id) on delete cascade,
  workspace_id uuid references workspaces(id) on delete cascade,
  user_id uuid not null references users(id) on delete cascade,
  role varchar(32) not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  unique (organization_id, workspace_id, user_id)
);

create table sessions (
  id uuid primary key,
  user_id uuid not null references users(id) on delete cascade,
  refresh_token_hash varchar(255) not null,
  device_name varchar(255),
  ip_address varchar(64),
  user_agent varchar(512),
  revoked_at timestamp,
  expires_at timestamp not null,
  created_at timestamp not null default current_timestamp
);

create table workflow_items (
  id uuid primary key,
  organization_id uuid not null references organizations(id) on delete cascade,
  workspace_id uuid not null references workspaces(id) on delete cascade,
  created_by_user_id uuid not null references users(id),
  title varchar(255) not null,
  description text not null,
  status varchar(32) not null,
  priority varchar(32) not null,
  assignee_user_id uuid references users(id),
  due_at timestamp,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);

create index idx_workflow_items_org_workspace_status
  on workflow_items (organization_id, workspace_id, status);

create index idx_workflow_items_org_workspace_updated_at
  on workflow_items (organization_id, workspace_id, updated_at desc);

create table workflow_events (
  id uuid primary key,
  workflow_item_id uuid not null references workflow_items(id) on delete cascade,
  organization_id uuid not null references organizations(id) on delete cascade,
  workspace_id uuid not null references workspaces(id) on delete cascade,
  event_type varchar(64) not null,
  old_value text,
  new_value text,
  actor_user_id uuid not null references users(id),
  created_at timestamp not null default current_timestamp
);

create table comments (
  id uuid primary key,
  workflow_item_id uuid not null references workflow_items(id) on delete cascade,
  organization_id uuid not null references organizations(id) on delete cascade,
  workspace_id uuid not null references workspaces(id) on delete cascade,
  user_id uuid not null references users(id),
  body text not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);

create table attachments (
  id uuid primary key,
  workflow_item_id uuid not null references workflow_items(id) on delete cascade,
  organization_id uuid not null references organizations(id) on delete cascade,
  workspace_id uuid not null references workspaces(id) on delete cascade,
  uploaded_by_user_id uuid not null references users(id),
  file_name varchar(255) not null,
  content_type varchar(128) not null,
  file_size bigint not null,
  storage_provider varchar(32) not null,
  storage_key varchar(1024) not null,
  checksum varchar(128),
  created_at timestamp not null default current_timestamp,
  deleted_at timestamp
);

create table notifications (
  id uuid primary key,
  organization_id uuid not null references organizations(id) on delete cascade,
  workspace_id uuid not null references workspaces(id) on delete cascade,
  user_id uuid not null references users(id) on delete cascade,
  type varchar(64) not null,
  title varchar(255) not null,
  body text not null,
  entity_type varchar(64) not null,
  entity_id uuid not null,
  read_at timestamp,
  created_at timestamp not null default current_timestamp
);

create index idx_notifications_user_read_at
  on notifications (user_id, read_at, created_at desc);

create table knowledge_documents (
  id uuid primary key,
  organization_id uuid not null references organizations(id) on delete cascade,
  workspace_id uuid not null references workspaces(id) on delete cascade,
  title varchar(255) not null,
  source_file_name varchar(255) not null,
  source_storage_key varchar(1024) not null,
  status varchar(32) not null,
  chunk_count integer not null default 0,
  indexed_at timestamp,
  failed_reason text,
  created_by_user_id uuid not null references users(id),
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);

create table knowledge_chunks (
  id uuid primary key,
  knowledge_document_id uuid not null references knowledge_documents(id) on delete cascade,
  organization_id uuid not null references organizations(id) on delete cascade,
  workspace_id uuid not null references workspaces(id) on delete cascade,
  chunk_index integer not null,
  content text not null,
  vector_id varchar(255),
  created_at timestamp not null default current_timestamp,
  unique (knowledge_document_id, chunk_index)
);

create table agent_conversations (
  id uuid primary key,
  organization_id uuid not null references organizations(id) on delete cascade,
  workspace_id uuid not null references workspaces(id) on delete cascade,
  user_id uuid not null references users(id) on delete cascade,
  title varchar(255) not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);

create table agent_messages (
  id uuid primary key,
  conversation_id uuid not null references agent_conversations(id) on delete cascade,
  role varchar(32) not null,
  content text not null,
  tool_name varchar(255),
  tool_payload text,
  created_at timestamp not null default current_timestamp
);

create table agent_memory_items (
  id uuid primary key,
  organization_id uuid not null references organizations(id) on delete cascade,
  workspace_id uuid not null references workspaces(id) on delete cascade,
  user_id uuid not null references users(id) on delete cascade,
  memory_type varchar(64) not null,
  content text not null,
  vector_id varchar(255),
  created_at timestamp not null default current_timestamp
);

