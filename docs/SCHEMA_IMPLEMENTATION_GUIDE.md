# PocketBase Schema Implementation Guide

This document outlines the schema design for the "bside" specific ORM and generic messaging SDK.

## Property System (Feature Toggles & Settings)

These collections provide a flexible way to manage user preferences and system-wide defaults.

### `t_tenant_property`
Stores global default values for properties/settings.
*   **Purpose**: Fallback for user properties.
*   **Indices**: Unique index on `key`.

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | Text (System) | Record ID |
| `key` | Text | The property name (e.g., `messaging.read_receipts_enabled`) |
| `value` | Text | The value (serialized as string) |
| `type` | Select | `boolean`, `string`, `int`, `json` |
| `description` | Text | Human-readable description |

### `t_user_property`
Stores user-specific overrides for properties.
*   **Purpose**: User toggles and settings.
*   **Indices**: Unique index on `(user_id, key)`.

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | Text (System) | Record ID |
| `user_id` | Relation | Relation to `users` (Single) |
| `key` | Text | The property name matching `t_tenant_property` |
| `value` | Text | The user's value |

## Messaging System (`m_`)

Real-time capable messaging collections.

### `m_conversations`
Represents a chat thread (direct or group).

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | Text (System) | Record ID |
| `type` | Select | `direct`, `group` |
| `name` | Text | Group name (empty for direct) |
| `last_message_at` | Date | Timestamp of the most recent message (for sorting) |
| `last_message_text` | Text | Preview of the last message |
| `metadata` | JSON | Extra data (icons, custom attributes) |
| `is_archived` | Bool | (Optional) Soft delete/archive status |

### `m_conversation_participants`
Links users to conversations with state.
*   **Indices**: Unique index on `(conversation_id, user_id)`. Index on `user_id` for list efficiency.

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | Text (System) | Record ID |
| `conversation_id` | Relation | Relation to `m_conversations` (Single, Cascade Delete) |
| `user_id` | Relation | Relation to `users` (Single, Cascade Delete) |
| `role` | Select | `admin`, `member` |
| `unread_count` | Number | Counter for unread messages |
| `last_read_message_id`| Relation | Relation to `m_messages` (max read message) |
| `joined_at` | Date | When user joined |
| `is_muted` | Bool | Mute notifications |
| `is_pinned` | Bool | Pin conversation to top |

### `m_messages`
Individual messages within a conversation.
*   **Indices**: Index on `(conversation_id, sent_at)` for pagination.

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | Text (System) | Record ID |
| `conversation_id` | Relation | Relation to `m_conversations` (Single, Cascade Delete) |
| `sender_id` | Relation | Relation to `users` (Single) |
| `content` | Text/Editor | Message text content |
| `type` | Select | `text`, `image`, `system` |
| `attachments` | File | Array of file attachments |
| `sent_at` | Date | Timestamp (often same as `created` but explicit) |
| `reply_to_message_id`| Relation | Relation to `m_messages` (Parent message) |
| `thread_root_id` | Relation | Relation to `m_messages` (Root of thread) |
| `thread_depth` | Number | Depth level interaction (0 for root) |
| `deleted_at` | Date | Soft delete timestamp |

### `m_read_receipts`
Detailed read status per message (granular).
*   **Note**: For high volume, consider aggregating updates or relying on `last_read_message_id` in participants. This collection is for "who read *this specific* message".
*   **Indices**: Unique index on `(message_id, user_id)`.

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | Text (System) | Record ID |
| `message_id` | Relation | Relation to `m_messages` (Single, Cascade Delete) |
| `user_id` | Relation | Relation to `users` (Single) |
| `read_at` | Date | When it was read |

### `m_typing_status`
Ephemeral or semi-persistent typing indicators.
*   **Mechanism**: Clients create/update their record. Clients subscribe to changes on this collection filtered by `conversation_id`.
*   **Indices**: Unique index on `(conversation_id, user_id)`.

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | Text (System) | Record ID |
| `conversation_id` | Relation | Relation to `m_conversations` (Single, Cascade Delete) |
| `user_id` | Relation | Relation to `users` (Single) |
| `is_typing` | Bool | Active status |
| `updated` | Date | Used to timeout stale typing status (System field) |

## Performance Indices Reference

You can copy these SQL statements directly into the "Indices" definition box in PocketBase.

### `t_tenant_property`
```sql
CREATE UNIQUE INDEX idx_tenant_key ON t_tenant_property (key)
```

### `t_user_property`
```sql
CREATE UNIQUE INDEX idx_user_key ON t_user_property (user_id, key)
```

### `m_conversation_participants`
```sql
CREATE UNIQUE INDEX idx_participant ON m_conversation_participants (conversation_id, user_id)
```
```sql
CREATE INDEX idx_user_conversations ON m_conversation_participants (user_id)
```

### `m_messages`
```sql
CREATE INDEX idx_conversation_sent ON m_messages (conversation_id, sent_at)
```

### `m_read_receipts`
```sql
CREATE UNIQUE INDEX idx_msg_read ON m_read_receipts (message_id, user_id)
```

### `m_typing_status`
```sql
CREATE UNIQUE INDEX idx_typing_status ON m_typing_status (conversation_id, user_id)
```

## API Rules (Permissions)

Use these rules to ensure basic security while maintaining flexibility. "Auth required" means `@request.auth.id != ""`.

| Collection | Rule Type | API Rule (SQL) | Purpose |
| :--- | :--- | :--- | :--- |
| **`t_tenant_property`** | List/View | `@request.auth.id != ""` | Auth users can read defaults. |
| | Create/Update/Delete | `null` | Admin only (manage defaults in dashboard). |
| **`t_user_property`** | List/View | `user_id = @request.auth.id` | Users see only their own settings. |
| | Create/Update | `user_id = @request.auth.id` | Users manage their own settings. |
| **`m_conversations`** | List/View | `@request.auth.id != ""` | Flexible: Auth users can see chats (UI filters relevant ones). |
| | Create | `@request.auth.id != ""` | Any auth user can start a chat. |
| | Update | `@request.auth.id != ""` | Members can update (e.g. name). |
| **`m_conversation_participants`**| List/View | `@request.auth.id != ""` | Auth users can see who is in chats. |
| | Create/Update | `@request.auth.id != ""` | Flexible: Allow adding members. |
| **`m_messages`** | List/View | `@request.auth.id != ""` | Auth users can read messages. |
| | Create | `sender_id = @request.auth.id` | **Critical**: Can only send as yourself. |
| | Update/Delete | `sender_id = @request.auth.id` | Can only edit/delete your own messages. |
| **`m_read_receipts`** | List/View | `@request.auth.id != ""` | See who read what. |
| | Create/Update | `user_id = @request.auth.id` | Can only mark as read for yourself. |
| **`m_typing_status`** | List/View | `@request.auth.id != ""` | See who is typing. |
| | Create/Update | `user_id = @request.auth.id` | Can only update your own status. |
