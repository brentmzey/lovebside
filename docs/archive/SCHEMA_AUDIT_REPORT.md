# Schema Audit & Correction Report

This report compares your current PocketBase schema against the standardized design. Follow the **Required Action** for each collection to align your database.

> [!IMPORTANT]
> **Naming Standard**: We are enforcing strict **snake_case** for ALL custom fields (e.g., `conversationId` → `conversation_id`).
> *   **System Fields**: `id`, `created`, `updated`, `username`, `email`, `emailVisibility` are system-defined and cannot be changed.
> *   **Your Fields**: MUST be `snake_case`.

---

## 1. `t_user` (System Auth Collection)
*ID: `_pb_users_auth_` (Often referred to as "users" but currently named `t_user`)*

**Status**: ⚠️ Custom fields need renaming to match the standard.

| Field | Current State | Required Change |
| :--- | :--- | :--- |
| `name` | `text` | ✅ Keep (System default) |
| `avatar` | `file` | ✅ Keep (System default - Simple File Upload) |
| `connectionType` | `select` | ✅ `connection_type` (Rename) |
| `completedProustQuestionnaire` | `bool` | ✅ `completed_proust_questionnaire` (Rename) |

**Indices**:
```sql
CREATE UNIQUE INDEX idx_user_token_key ON t_user (tokenKey)
```
```sql
CREATE UNIQUE INDEX idx_user_email ON t_user (email)
```

**API Rules**:
*   **List/View**: `id = @request.auth.id` (Privacy: only view yourself)
*   **Create**: `null` (Registration disabled? Or Public if open reg)
*   **Update**: `id = @request.auth.id` (Update self)
*   **Delete**: `id = @request.auth.id` (Delete self)

---

## 2. `t_tenant_property` (NEW)
**Status**: ❌ Missing entirely.
**Action**: Create this collection.

| Field Name | Type | Options |
| :--- | :--- | :--- |
| `key` | Text | Required, Unique |
| `value` | Text | Required |
| `type` | Select | Values: `boolean, string, int, json` |
| `description` | Text | - |

**Indices**:
```sql
CREATE UNIQUE INDEX idx_tenant_key ON t_tenant_property (key)
```

**API Rules**:
*   **List/View**: `@request.auth.id != ""` (Auth users can read)
*   **Create/Update/Delete**: `@request.auth.id != ""` (Allow any auth user to manage defaults for now)

---

## 3. `t_user_property` (NEW)
**Status**: ❌ Missing entirely.
**Action**: Create this collection.

| Field Name | Type | Options |
| :--- | :--- | :--- |
| `user_id` | Relation | `t_user` (Single, Cascade) |
| `key` | Text | Required |
| `value` | Text | Required |

**Indices**:
```sql
CREATE UNIQUE INDEX idx_user_key ON t_user_property (user_id, key)
```

**API Rules**:
*   **List/View**: `user_id = @request.auth.id`
*   **Create/Update**: `user_id = @request.auth.id`
*   **Delete**: `user_id = @request.auth.id`

---

## 4. `m_conversations`
**Status**: ⚠️ Exists but needs field updates (Renaming).

| Field | Current State | Required Change |
| :--- | :--- | :--- |
| `conversationType` | `select` | 📝 Rename to `type` |
| `conversationName` | `text` | 📝 Rename to `name` |
| `conversationAvatar` | `file` | ✅ `avatar` (Rename to shorter `avatar` and KEEP as **File** type) |
| `lastMessageText` | `text` | ✅ `last_message_text` (Rename to snake_case) |
| `lastMessageAt` | `date` | ✅ `last_message_at` (Rename to snake_case) |
| `totalMessageCount` | `number` | ❌ Delete (Compute on client or generic count) |
| `maxParticipants` | `number` | ❌ Delete (Not needed for MVP) |
| `isArchived` | `bool` | ✅ `is_archived` (Rename to snake_case) |

**API Rules**:
*   **List/View**: `@request.auth.id != ""`
*   **Create**: `@request.auth.id != ""`
*   **Update**: `@request.auth.id != ""`
*   **Delete**: `@request.auth.id != ""`

---

## 5. `m_conversation_participants`
**Status**: ⚠️ Exists but needs field updates.

| Field | Current State | Required Change |
| :--- | :--- | :--- |
| `conversationId` | `relation` | ✅ `conversation_id` (Rename) |
| `userId` | `relation` | ✅ `user_id` (Rename) |
| `role` | `select` | ✅ Keep (Ensure values: `admin`, `member`) |
| `unreadCount` | `number` | ✅ `unread_count` (Rename) |
| `lastReadAt` | `date` | ❌ Delete (Replaced by `last_read_message_id` pointer) |
| **NEW** | - | ➕ Add `last_read_message_id` (Relation to `m_messages`) |
| `joinedAt` | `date` | ✅ `joined_at` (Rename) |
| `leftAt` | `date` | ❌ Delete (Soft delete handled by record deletion) |
| `isMuted` | `bool` | ✅ `is_muted` (Rename) |
| `isPinned` | `bool` | ✅ `is_pinned` (Rename) |

**Indices**:
```sql
CREATE UNIQUE INDEX idx_participant ON m_conversation_participants (conversation_id, user_id)
```
```sql
CREATE INDEX idx_user_conversations ON m_conversation_participants (user_id)
```

**API Rules**:
*   **List/View**: `@request.auth.id != ""`
*   **Create/Update**: `@request.auth.id != ""`
*   **Delete**: `@request.auth.id != ""`

---

## 6. `m_messages`
**Status**: ⚠️ Exists but needs field updates.

| Field | Current State | Required Change |
| :--- | :--- | :--- |
| `conversationId` | `relation` | ✅ `conversation_id` (Rename) |
| `senderId` | `relation` | ✅ `sender_id` (Rename) |
| `content` | `text` | 📝 Change Type to `Editor` (Better for rich text) |
| `messageType` | `select` | ✅ `type` (Rename. Values: `text`, `image`, `system`) |
| `attachments` | `file` | ✅ Keep |
| `sentAt` | `date` | ✅ `sent_at` (Rename) |
| `editedAt` | `date` | ❌ Delete (Not in MVP spec yet) |
| `deletedAt` | `date` | ✅ `deleted_at` (Rename) |
| `readByCount` | `number` | ❌ Delete (Use `m_read_receipts` or participant counts) |
| `replyToMessageId`| `relation` | ✅ `reply_to_message_id` (Rename) |
| `threadRootId` | `text` | 📝 Change Type to `Relation` (Points to `m_messages`) & Rename `thread_root_id` |
| `threadDepth` | `number` | ✅ `thread_depth` (Rename) |
| `threadReplyCount`| `number` | ❌ Delete |

**Indices**:
```sql
CREATE INDEX idx_conversation_sent ON m_messages (conversation_id, sent_at)
```

**API Rules**:
*   **List/View**: `@request.auth.id != ""`
*   **Create**: `sender_id = @request.auth.id`
*   **Update**: `sender_id = @request.auth.id`
*   **Delete**: `sender_id = @request.auth.id`

---

## 7. `m_read_receipts`
**Status**: ⚠️ Exists but needs field updates.

| Field | Current State | Required Change |
| :--- | :--- | :--- |
| `messageId` | `relation` | ✅ `message_id` (Rename) |
| `userId` | `relation` | ✅ `user_id` (Rename) |
| `readAt` | `date` | ✅ `read_at` (Rename) |

**Indices**:
```sql
CREATE UNIQUE INDEX idx_msg_read ON m_read_receipts (message_id, user_id)
```

**API Rules**:
*   **List/View**: `@request.auth.id != ""`
*   **Create/Update**: `user_id = @request.auth.id`
*   **Delete**: `user_id = @request.auth.id`

---

## 8. `m_typing_status`
**Status**: ⚠️ Exists but needs field updates.

| Field | Current State | Required Change |
| :--- | :--- | :--- |
| `typingUser` | `relation` | ❌ Delete (Dup of `userId`) |
| `conversationId` | `relation` | ✅ `conversation_id` (Rename) |
| **NEW** | - | ➕ Add `user_id` (Relation to `t_user`) |
| `isTyping` | `bool` | ✅ `is_typing` (Rename) |

**Indices**:
```sql
CREATE UNIQUE INDEX idx_typing_status ON m_typing_status (conversation_id, user_id)
```

**API Rules**:
*   **List/View**: `@request.auth.id != ""`
*   **Create/Update**: `user_id = @request.auth.id`
*   **Delete**: `user_id = @request.auth.id`

---

## 9. `m_matches`
**Status**: ⚠️ Exists but needs field updates.

| Field | Current State | Required Change |
| :--- | :--- | :--- |
| `userId` | `relation` | ✅ `user_id` (Rename) |
| `matchedUserId` | `relation` | ✅ `matched_user_id` (Rename) |
| `matchScore` | `number` | ✅ `match_score` (Rename) |
| `status` | `select` | ✅ Keep |

**Indices**:
```sql
CREATE UNIQUE INDEX idx_match_pair ON m_matches (user_id, matched_user_id)
```

**API Rules**:
*   **List/View**: `user_id = @request.auth.id`
*   **Create**: `user_id = @request.auth.id`
*   **Update**: `user_id = @request.auth.id`
