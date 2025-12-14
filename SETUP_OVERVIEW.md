# 🚀 Quick Setup & Test Overview

## 1️⃣ Spin up PocketBase
- `pocketbase serve` (or use the hosted instance at `https://bside.pockethost.io/`).
- Import the schema: `pocketbase import ./pb_schema.json`.
- Verify collections appear in the admin UI.

## 2️⃣ Build the SDK
```bash
cd /Users/brentzey/bside
./gradlew clean build   # compiles all modules
```
- Ensure the build succeeds (no compilation errors).

## 3️⃣ Run Tests
```bash
./gradlew test          # unit + integration tests
```
- All tests must pass and coverage ≥ 90 % (JaCoCo report).

## 4️⃣ Smoke‑Check the API
1. **Auth** – use a test user (`test@example.com` / `test12345`).
2. **Create a direct conversation** via `MessagingRepository.createDirectConversation`.
3. **Send a message**, then **fetch the thread** (`getFullThread`).
4. Verify no exceptions and that data is persisted.

---

## 📚 Detailed Guides (for deeper setup / debugging)
- **Schema import & index verification** – see `collection_setup_instructions.md`.
- **Repository implementation details** – see `complete_repository_implementation.kt`.
- **Threading design** – see `message_threading_design.md`.
- **CI/CD pipeline** – see `production_readiness.md`.
- **Full checklist** – see `SETUP_CHECKLIST.md` (the exhaustive list you can tick off).
- **Debugging tips** – see `complete_setup_guide.md` for common pitfalls (e.g., `Either` vs `Result`, missing imports, index errors).

---

### How to Use
1. Open this file, follow the **Quick Setup** steps to get a running backend.
2. Run the tests; if any fail, consult the linked detailed guides.
3. Once the quick steps succeed, tick off items in `SETUP_CHECKLIST.md` for full coverage.

Happy coding! 🎉
