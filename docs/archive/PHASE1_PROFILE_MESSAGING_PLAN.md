# Phase 1: Profile Management + 1-on-1 Messaging

## Current Status (October 19, 2025)

### ✅ Completed
- Messaging data models (Conversation, Message, TypingIndicator, ReadReceipt)
- MessagingRepository with caching and real-time subscriptions
- MessagingApiClient with REST endpoints
- PocketBase messaging migrations (m_conversations, m_messages, m_typing_indicators, m_read_receipts)
- Profile data model (basic fields: firstName, lastName, birthDate, bio, location, seeking)
- Basic app structure with Decompose navigation

### ❌ To Build

#### 1. Profile Pictures & File Uploads (Backend + Shared)
- [ ] Add `profilePicture` field to s_profiles collection (file type)
- [ ] Add `photos` field to s_profiles collection (multiple files)
- [ ] Create FileUploadApiClient in shared/data/api
- [ ] Add profile picture upload to ProfileRepository
- [ ] Update Profile model to include avatar/photos URLs

#### 2. Profile Management UI (ComposeApp)
- [ ] ProfileViewScreen - View own/other profiles
- [ ] ProfileEditScreen - Edit own profile with image upload
- [ ] ImagePickerComponent - Platform-specific image selection
- [ ] ProfileViewModel - State management for profiles
- [ ] Profile photo gallery/carousel component

#### 3. Messaging UI (ComposeApp)
- [ ] ConversationsListScreen - List all conversations
- [ ] ChatScreen - 1-on-1 messaging interface
- [ ] MessagingViewModel - State management
- [ ] MessageBubbleComponent - Individual message UI
- [ ] TypingIndicatorComponent - Real-time typing status
- [ ] MessageInputComponent - Text input with send button
- [ ] Real-time message subscription integration

#### 4. Navigation Updates
- [ ] Add Profile route to RootComponent
- [ ] Add Messaging routes (list + chat)
- [ ] Add deep linking for conversations
- [ ] Bottom navigation bar for Main screen

#### 5. Image Handling (Platform-Specific)
- [ ] Android: Image picker + camera integration
- [ ] iOS: Photo library + camera integration  
- [ ] Web: File input + drag-drop
- [ ] JVM: File chooser dialog
- [ ] Image compression/optimization before upload

## Implementation Order

### Step 1: Profile Pictures Backend (30 min)
1. Update PocketBase schema migration for profile pictures
2. Add file upload endpoints to server
3. Update Profile model and mapper

### Step 2: File Upload Infrastructure (45 min)
1. Create FileUploadApiClient
2. Add multipart form upload support
3. Platform-specific image picker interfaces
4. Test file upload flow

### Step 3: Profile UI (2 hours)
1. ProfileViewModel with edit/view states
2. ProfileViewScreen layout
3. ProfileEditScreen with form fields
4. Image picker integration
5. Save/cancel functionality

### Step 4: Messaging UI Foundation (2 hours)
1. ConversationsListScreen with conversation items
2. ChatScreen basic layout
3. MessagingViewModel state management
4. Message list rendering

### Step 5: Real-time Messaging (1.5 hours)
1. MessageInputComponent with typing detection
2. Real-time message subscriptions
3. Typing indicators
4. Read receipts
5. Message sending/receiving

### Step 6: Polish & Testing (1 hour)
1. Error handling
2. Loading states
3. Empty states
4. Scroll to bottom on new messages
5. Message timestamps

## Technical Notes

### File Upload Strategy
- Use PocketBase's built-in file upload (supports images, validation, thumbnail generation)
- Profile pictures: Single file field, max 5MB, auto-resize to 512x512
- Gallery photos: Multiple file field, max 10 images, 10MB each
- Return CDN URLs in API responses

### Real-time Messaging
- PocketBase SSE (Server-Sent Events) for real-time subscriptions
- Subscribe to m_messages collection filtered by conversationId
- Subscribe to m_typing_indicators for typing status
- Auto-reconnect on connection loss

### Image Optimization
- Client-side resize before upload (reduce bandwidth)
- WebP format where supported
- Progressive JPEG fallback
- Lazy loading for message images

### State Management
- Use Kotlin Flow for reactive data
- Cache profile data (5 min TTL)
- Cache conversations (30 sec TTL)
- Cache messages per conversation (1 min TTL)
- Real-time updates invalidate caches

## Next Phases

**Phase 2:** Matching Algorithms (2-3 hours)
- Affinity scoring based on shared values
- Proust questionnaire similarity analysis
- Match generation background job
- Match recommendations UI

**Phase 3:** UI/UX Branding (3-4 hours)
- Custom theme with brand colors
- Logo integration
- Custom iconography
- Animations and transitions
- Improved typography
- Dark mode support
