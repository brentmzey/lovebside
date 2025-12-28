# 📋 PocketBase Messaging SDK – Setup & Verification Checklist

This markdown file contains a **checkable list** you can use to track progress while getting the SDK ready for production. Open the file in your editor and tick the boxes (`[ ]` → `[x]`) as you complete each step.

---

## 1️⃣ Prepare PocketBase Instance
- [ ] Start a PocketBase server (local or hosted at `https://bside.pockethost.io/`).
- [ ] Import the schema (`pb_schema.json`) into the admin UI or via CLI:
  ```bash
  pocketbase import ./pb_schema.json
  ```
- [ ] Verify the following collections were created (match the JSON you provided):
  - `_superusers`
  - `t_user`
  - `m_conversation_participants`
  - `m_conversations`
  - `m_messages`
  - `m_read_receipts`
  - `s_matches`
  - `s_profiles`
  - `t_message`
  - `t_proust_question`
  - `t_proust_questionnaire`
  - `t_typing_status`
  - `t_user_questionnaire_responses`
- [ ] Confirm indexes for each collection (see the JSON for index definitions).

## 2️⃣ Align Kotlin SDK Imports & Types
- [x] Add missing PocketBase imports in `PocketBaseMessagingRepository.kt`:
  ```kotlin
  import io.pocketbase.PocketBase
  import io.pocketbase.functional.getListTyped
  import io.pocketbase.functional.getOneTyped
  import io.pocketbase.models.ListResult
  import io.pocketbase.models.QueryOptions
  import io.pocketbase.models.RecordModel
  ```
- [x] Ensure the custom `Result` sealed class (`Success`, `Error`, `Loading`) is used consistently.
- [x] Remove any remaining Arrow/Either usage or decide to keep a single error‑handling model.

## 3️⃣ Fix Compilation Errors
- [x] Replace `totalItems` / `items` references with `ListResult<T>` properties.
- [x] Explicitly specify generic types for `getListTyped` / `getOneTyped`, e.g. `getListTyped<io.pocketbase.types.Message>(…)`.
- [x] Convert any `Either` values to `Result` (or drop Arrow entirely).
- [x] Run `./gradlew clean test` and confirm **no compilation errors**.

## 4️⃣ Verify Core Repository Methods
| Method | Checklist |
|--------|-----------|
| `getConversations` | ✅ Returns `Result<List<Conversation>>` with participants expanded |
| `getConversation` | ✅ Retrieves a single conversation by ID |
| `createDirectConversation` | ✅ Creates conversation + participants, validates exactly 2 IDs |
| `getParticipants` / `addParticipants` | ✅ Handles participant CRUD, respects unique index |
| `getMessages` | ✅ Uses correct filter & pagination |
| `sendMessage` | ✅ Creates message, updates conversation's `lastMessage*` fields, returns domain model |
| Threading (`getReplies`, `getThreadRoot`, `getFullThread`, `countReplies`) | ✅ Uses `replyToMessageId` and thread fields; `countReplies` reads `listResult.totalItems` |
| Advanced queries (`searchMessages`, `getMessagesAfter`, `getMessagesBefore`) | ✅ Correct filter syntax (`content~'query'`, `sentAt>…`, `sentAt<…`) |
| Real‑time placeholders | ✅ Implemented polling‑based `RealtimeServiceImpl` |
- [x] Run the existing **unit tests** (`PocketBaseMessagingRepositoryUnitTest.kt`).
- [x] Run **integration tests** against the live PocketBase instance (`FullMessagingIntegrationTest.kt`).

## 5️⃣ Ensure Indexes & Performance
- [ ] Verify each collection’s indexes match the JSON (use admin UI or `SELECT * FROM _schema`).
- [ ] Perform a quick benchmark (e.g., fetch 100 messages, count replies) and confirm queries use indexes (check PocketBase logs for `EXPLAIN`).

## 6️⃣ CI/CD Pipeline
- [ ] Add a GitHub Actions workflow (`.github/workflows/ci.yml`) that:
  1. Checks out code.
  2. Sets up JDK 17.
  3. Runs `./gradlew clean test`.
  4. Publishes JaCoCo coverage (`./gradlew jacocoTestReport`).
  5. (Optional) Deploys migration scripts via `deploy-to-pockethost.js`.
- [ ] Verify the pipeline passes on a fresh PR.

## 7️⃣ Documentation & Release Checklist
- [ ] Update **README** with sections: project overview, how to start PocketBase, SDK usage examples.
- [ ] Generate Kotlin Dokka docs (`./gradlew dokkaHtml`).
- [ ] Bump version and tag a release (e.g., `v1.0.0`).
- [ ] Add a **CHANGELOG** summarizing the migration to `Result` handling and other major changes.

## 8️⃣ Final Validation
- [ ] Run the full test suite (`./gradlew clean test`) – all tests must pass.
- [ ] Verify code coverage ≥ 90 % (JaCoCo report).
- [ ] Perform a manual smoke test:
  1. Authenticate a test user.
  2. Create a direct conversation.
  3. Send a message, reply, fetch the full thread.
  4. Ensure no runtime errors.
- [ ] Deploy the latest version using the deployment script.

---

**How to use:**
1. Open this file in your editor.
2. Replace each `[ ]` with `[x]` as you complete the step.
3. Commit the checklist file when you’re done so the team can see the progress.

Good luck! 🎉
