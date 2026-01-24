# Project Process & Tracking

## 🔄 Notion Sync

Since the AI agent cannot access Notion directly, we maintain a `.code-hq` directory as the source of truth for technical progress. To keep Notion up to date:

1. **Weekly**: Copy the status from `.code-hq/KANBAN.md` to the Notion Kanban board.
2. **Epics**: Update Notion Roadmap pages when `ROADMAP_PHASE_2.md` is updated.
3. **Documentation**: When `docs/` artifacts change, export them to Notion DB.

## 🛠 Active Development Flow

1. **Pick a Task**: Move item to "In Progress" in `.code-hq/KANBAN.md`.
2. **Create Branch**: `feature/task-name`.
3. **Implementation**: Follow TDD (write test -> fail -> pass).
4. **Verification**: Update `walkthrough.md` with proof of work.
5. **Review**: Submit PR and move to "Review" in Kanban.

## 🧪 Testing Strategy

- **Unit Tests**: `src/commonTest`. Run via `./gradlew test`.
- **Integration Tests**: `src/jvmTest`. Requires local PocketBase.
  - Ensure `tester_admin@bside.love` exists.
  - Run `./gradlew :shared:jvmTest`.
