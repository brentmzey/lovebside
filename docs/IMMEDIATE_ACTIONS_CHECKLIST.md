# Immediate Actions Checklist

## 1️⃣ Code – SDK Ready for Integration Tests
- Add `findRoot` & `findDepth` helpers in `PocketBaseMessagingRepository.kt`.
- Set `threadRootId`, `threadDepth`, `threadReplyCount` on `sendMessage`.
- Provide no‑op compile‑safe implementations for `subscribeToConversation` and `subscribeToTypingIndicators`.
- Ensure required imports (`Instant`, `Clock`, `Result`, `AppException`).
- Run `./gradlew clean shared:compileKotlinJvm` – no errors.
- Run unit tests: `./gradlew shared:test` – all pass, JaCoCo ≥ 90 %.

## 2️⃣ PocketBase / PocketHost – Schema & Indexes
- Verify all collections (including `t_typing_status`) exist in the admin UI.
- Add covering index for deep thread look‑ups:
  ```sql
  CREATE INDEX idx_msg_thread_depth ON m_message (threadRootId, threadDepth, sentAt ASC) WHERE deletedAt IS NULL;
  ```
- Enable SQLite WAL mode (`PRAGMA journal_mode=WAL;`).
- Create a service‑account token with Admin scope; store as `POCKETBASE_TOKEN`.
- (Optional) Add a simple health endpoint returning `OK`.

## 3️⃣ Integration Tests – Run Against Live DB
- Create/Update `MessagingThreadingIntegrationTest.kt` with full conversation‑thread flow and a 10k‑message stress test.
- Export token: `export POCKETBASE_TOKEN=$(cat .env | grep POCKETBASE_TOKEN | cut -d'=' -f2)`.
- Run: `./gradlew shared:test --tests "*IntegrationTest"`.
- Open HTML report to confirm all pass.

## 4️⃣ CI / CD – Hook Everything Up
- Add `POCKETBASE_TOKEN` secret to GitHub.
- Update `.github/workflows/ci.yml` to run unit + integration tests, upload JaCoCo and test reports.
- Add a deployment step (`node scripts/deploy-to-pockethost.js`).
- Add a post‑deployment health‑check (`curl -f https://bside.pockethost.io/health`).

## 5️⃣ Compose Multiplatform UI – Quick Integration
- Create shared `MessagingViewModel` that injects `MessagingRepository`.
- Use the ViewModel in Android & iOS Compose screens.
- Verify UI shows messages, typing indicator, and respects premium design.

## 6️⃣ Final Production Steps
- Tag a release, publish KMP library, rotate token, monitor logs, update docs.

**Next:** Run the code changes (steps 1‑2), then execute the integration tests (step 3) and push the CI updates (step 4). Once all green, you have a fully testable, production‑ready messaging stack.
