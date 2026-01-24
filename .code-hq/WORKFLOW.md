# Code-HQ Workflow Guide

## Overview

The `.code-hq/` directory is the **central coordination hub** for the B-Side project. It enables seamless collaboration between AI agents (Claude, Gemini) and human developers.

---

## 📂 File Structure

```
.code-hq/
├── CONTEXT.md          # Current project status & recent work
├── KANBAN.md           # Task board (P0, P1, Done)
├── STORIES.md          # Detailed user stories with acceptance criteria
├── AGENT_HANDOFF.md    # Protocol for session start/end
├── WORKFLOW.md         # This file - how to use the system
└── board.js            # CLI tool for status/metrics
```

---

## 🎯 Daily Workflow

### 1. **Orient** (Start of Session)

```bash
# Quick status check
node .code-hq/board.js status

# Read in order:
cat .code-hq/CONTEXT.md    # What's the current state?
cat .code-hq/KANBAN.md     # What should I work on?
cat .code-hq/STORIES.md    # What are the acceptance criteria?
```

### 2. **Work** (During Session)

- Pick a task from **KANBAN.md → P0 Section**
- Mark it `[/]` in `task.md` when starting
- Make code changes
- Update documentation as you go
- Mark it `[x]` in `task.md` when done

### 3. **Update** (After Completing Work)

```bash
# Move completed items in KANBAN.md:
- From "🔄 NEXT" section
- To "✅ Done" section

# Update CONTEXT.md:
- Add session notes under "Recent Session Notes"
- Update "Last Updated" timestamp

# Check status:
node .code-hq/board.js stories
```

### 4. **Handoff** (End of Session)

- Update `CONTEXT.md` with what's next
- Commit your work
- Notify user with summary

---

## 📋 Moving Stories Through the Board

### Story States

- **📋 TODO** - Not started (0% complete)
- **🔄 IN PROGRESS** - Some tasks done (1-99% complete)
- **✅ DONE** - All acceptance criteria met (100% complete)

### How to Move a Story

#### From TODO → IN PROGRESS

1. Open `STORIES.md`
2. Find the story
3. Start working on first task
4. Update `task.md` with `[/]` for in-progress

#### From IN PROGRESS → DONE

1. Complete all tasks in story
2. Mark all `[x]` in `task.md`
3. Move story to ✅ Done in `KANBAN.md`
4. Update `CONTEXT.md` with completion

---

## 🤖 For AI Agents

### Session Start Checklist

- [ ] Run `node .code-hq/board.js status`
- [ ] Read `CONTEXT.md` (current state)
- [ ] Check `KANBAN.md` P0 section
- [ ] Review relevant story in `STORIES.md`

### During Work

- [ ] Update `task.md` with `[/]` when starting task
- [ ] Make code changes
- [ ] Update docs (README, RUNNING, etc.)
- [ ] Mark `[x]` in `task.md` when done

### Session End Checklist

- [ ] Move completed items in `KANBAN.md`
- [ ] Update `CONTEXT.md` session notes
- [ ] Commit changes
- [ ] Notify user with summary

---

## 📊 Using the Board Manager

```bash
# Show dashboard with metrics
node .code-hq/board.js status

# List all stories with progress
node .code-hq/board.js stories

# Show help
node .code-hq/board.js help
```

### Sample Output (status)

```
════════════════════════════════════════
   📊 B-Side Project Dashboard
════════════════════════════════════════

✅ Completed Tasks: 42/68
🔄 In Progress:     3
📋 To Do:           23

P0 Critical Path:
  ✅ Done: 4 items
  🔄 Next: 2 items
  🔄 After: 5 items

Current Focus:
  Nginx Smart Routing configuration

Last Session: 2026-01-03
  - Implemented idempotent migration system
  - Created initial schema migration
  - **Next**: Nginx routing config
```

---

## 🔄 Syncing Between Agents

When multiple agents (Claude, Gemini) work on the same project:

1. **Always read `CONTEXT.md` first**
2. **Never assume** - check the current state
3. **Update as you go** - don't leave stale info
4. **Communicate clearly** - update session notes
5. **Test before marking done** - verify your work

---

## 📝 Best Practices

### DO ✅

- Read CONTEXT.md at session start
- Update task.md incrementally
- Move completed items in KANBAN.md
- Add session notes to CONTEXT.md
- Run board.js to verify state

### DON'T ❌

- Skip reading CONTEXT.md
- Leave tasks marked `[/]` without completing
- Forget to update CONTEXT.md
- Assume previous state without checking
- Work on P1 when P0 tasks exist

---

## 🆘 Troubleshooting

### "I don't know what to work on"

→ Check `KANBAN.md` P0 section, look for "🔄 NEXT"

### "Previous agent left mid-task"

→ Check `task.md` for `[/]` items and `CONTEXT.md` for notes

### "Build is broken"

→ STOP feature work, fix build first (see AGENT_HANDOFF.md)

### "Unclear requirements"

→ Check `STORIES.md` acceptance criteria, ask user if still unclear

---

## 🔗 Quick Links

- [Project README](file:///Users/brentzey/bside/README.md)
- [Running Guide](file:///Users/brentzey/bside/docs/RUNNING.md)
- [Schema Strategy](file:///Users/brentzey/bside/docs/SCHEMA_HARDENING.md)
- [Task Checklist](file:///Users/brentzey/.gemini/antigravity/brain/e9de2573-c41f-4db6-a3c5-5597c4bd61fe/task.md)
