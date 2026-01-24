# Agent Handoff Protocol

## Purpose

This document ensures smooth transitions between AI agents (Claude, Gemini, etc.) working on the B-Side project.

---

## 🔄 Starting a New Session

### 1. Orient Yourself (5 min)

```bash
# Read these files IN ORDER:
1. .code-hq/CONTEXT.md          # Current state, recent work
2. .code-hq/KANBAN.md            # What to work on next
3. .code-hq/STORIES.md           # Detailed requirements
4. task.md (in artifacts)        # Granular checklist
```

### 2. Verify Environment

```bash
# Ensure you can build and run:
./gradlew :composeApp:assemble   # Should pass
just migrate-status               # Should connect to DB
```

### 3. Identify Next Task

- Look for items in **KANBAN.md → P0 Section**
- Check if there are "🔄 NEXT" markers
- Confirm with user if unclear

---

## 📝 During Work

### Update Tracking Files

- **Real-time**: Mark items `[/]` in `task.md` when starting
- **After completion**: Mark `[x]` in both `task.md` AND `KANBAN.md`
- **Blockers**: Add to KANBAN immediately with ⚠️ emoji

### Documentation Rule

For every code change, update ONE of:

- README.md (if it changes how to run/use)
- RUNNING.md (if it changes commands)
- SCHEMA_HARDENING.md (if it's DB-related)
- STORIES.md (if acceptance criteria change)

### Commit Messages

Use conventional commits:

```
feat(migrations): add initial schema migration
fix(nginx): correct FQDN routing config
docs(readme): update quick start guide
```

---

## ✅ Ending a Session

### 1. Update CONTEXT.md

```markdown
### [DATE] Session ([Agent Name])
- [What was accomplished]
- [What was tested]
- **Next**: [What should be done next]
```

### 2. Update KANBAN.md

- Move completed P0 items to "✅ Done"
- Update "🔄 NEXT" marker
- Add any new blockers to 🚨 section

### 3. Commit Your Work

```bash
git add .
git commit -m "feat(schema): complete idempotent migration system"
git push origin main
```

### 4. Notify User

Use `notify_user` with:

- **What's complete** (with ✅)
- **What's tested** (evidence/screenshots if UI)
- **What's next** (clear action items)
- **Blockers** (if any)

---

## 🎯 Priority Matrix

When multiple tasks are available, prioritize:

1. **P0 Blockers** - Fix these FIRST
2. **P0 Tasks** - Core functionality
3. **Build Failures** - Must be green
4. **P1 Tasks** - Nice to have
5. **Tech Debt** - Only if time permits

---

## 🔧 Common Scenarios

### Scenario: Previous agent left mid-task

1. Read their last session notes in CONTEXT.md
2. Check `task.md` for `[/]` (in-progress) items
3. Review git history: `git log -5 --oneline`
4. If unclear, ask user before proceeding

### Scenario: Build is broken

1. **STOP** all feature work
2. Read error messages carefully
3. Check recent commits: `git diff HEAD~1`
4. Fix build FIRST, then continue

### Scenario: Unclear requirements

1. Check STORIES.md for acceptance criteria
2. Check implementation_plan.md for design decisions
3. If still unclear, ask user (don't assume)

---

## 📚 Quick Reference

### Key Files

- `Justfile` - All run commands
- `task.md` - Granular checklist (artifacts)
- `KANBAN.md` - High-level board
- `STORIES.md` - User stories with acceptance criteria
- `CONTEXT.md` - Project state snapshot

### Key Commands

```bash
just up              # Start backend
just migrate         # Run migrations
just web            # Run web client
./gradlew assemble  # Build all targets
```

### Emergency Contacts

- **Build Issues**: Check `docs/RUNNING.md`
- **Schema Issues**: Check `docs/SCHEMA_HARDENING.md`
- **Migration Issues**: Check `pocketbase/migrations/README.md`

---

## 💡 Tips for Success

1. **Read before writing** - Understand existing code/docs first
2. **Small commits** - Each logical change separately
3. **Test as you go** - Don't accumulate untested code
4. **Update docs** - Future agents (and users) will thank you
5. **Ask when stuck** - 5 min of clarification > 30 min wrong direction
