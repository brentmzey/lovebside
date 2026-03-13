# Messaging Features Implementation Plan

## ✅ Completed Backend & Schema
- [x] **Reactions Schema**: `m_reactions` collection created and verified.
- [x] **Presence Schema**: `m_presence` collection created and verified.
- [x] **Repository Updates**:
    - `MessagingRepository` supports `m_conversations` (V2).
    - `addReaction` / `removeReaction` implemented.
    - `setPresence` / `getPresence` implemented.
    - `getMessages` expands reactions.
- [x] **Models**:
    - `Reaction` model created.
    - `Presence` model created.
    - `Message` model updated with `reactions` list.
- [x] **Verification**:
    - `scripts/verify-messaging-backend.sh` created for standalone verification.
    - Integration tests passed.

## ✅ Completed UI Foundation
- [x] **Reactions Display**: `MessageBubble` now supports rendering reactions.
- [x] **Presence Indicator**: `ConversationListScreen` has a visual indicator for online status.

## 🚧 Next Steps (User Interface & Integration)

### 1. Reactions Interaction
- **Goal**: Allow users to add/remove reactions.
- **Tasks**:
    - Create a `ReactionPicker` component (bottom sheet or popup menu with emojis).
    - Connect `onLongClick` on `MessageBubble` to show the picker.
    - Call `viewModel.addReaction(messageId, emoji)` when selected.
    - Update `ChatViewModel` to handle the repository call.

### 2. Real Presence Data
- **Goal**: Show actual online status instead of the placeholder.
- **Tasks**:
    - Update `MessagingRepository.getConversations` to expand/fetch presence for participants.
    - Or subscribe to `m_presence` changes in real-time.
    - Update `ConversationListScreen` to use the real data.

### 3. Typing Indicators
- **Goal**: Show "User is typing..."
- **Tasks**:
    - Implement `m_typing_status` subscription in `ChatViewModel`.
    - Send typing events on text input change (debounced).
    - Show typing indicator in `ChatScreen`.

### 4. Read Receipts
- **Goal**: Show double ticks when read.
- **Tasks**:
    - Call `markAsRead` when message enters viewport.
    - Update UI based on `is_read` field (already supported in `MessageBubble`, just need to ensure data flow).

## How to Verify Current State
Run the verification script to confirm the backend is solid:
```bash
./scripts/verify-messaging-backend.sh
```
