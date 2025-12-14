# code-hq Agent Guide

**For AI Agents**: This project uses **code-hq** for semantic project management.

## What is code-hq?

code-hq is a local knowledge graph that tracks:
- **Tasks** - Work items with status, priority, assignees, dependencies
- **Notes** - Decisions, meetings, research, ideas
- **People** - Team members and roles
- **Milestones** - Project goals and deadlines

All data is stored in `.code-hq/graph.jsonld` (JSON-LD format) and queryable via CLI.

## Quick Reference

```bash
# List entities
code-hq tasks                    # All tasks
code-hq tasks --status blocked   # Filtered tasks
code-hq notes                    # All notes

# Create entities
code-hq create task "Title" --priority high --assignee person:alice
code-hq create note "Title" --note-type decision --content "..."

# Update entities
code-hq update task:123 --status done
code-hq update task:123 --actual-hours 4

# Query (Datalog)
code-hq query "FIND task WHERE ?t.status = 'blocked' RETURN ?t"

# Validate
code-hq validate
```

## Available Guides

- **[task-management.md](./task-management.md)** - Complete guide to managing tasks
- **[note-taking.md](./note-taking.md)** - How to document decisions and research
- **[query-examples.md](./query-examples.md)** - Common queries and patterns
- **[workflows/](./workflows/)** - Automation workflows

## IDE Integration

### Cursor
Add to `.cursorrules`:
```markdown
This project uses code-hq for task management.
Reference .code-hq/prompts/task-management.md for commands.
Always check existing tasks before creating duplicates.
Update task status when starting/finishing work.
```

### Windsurf
Create `.windsurf/workflows/codehq.md`:
```markdown
---
description: code-hq commands reference
---
Read `.code-hq/prompts/_index.md` for overview.
See `.code-hq/prompts/task-management.md` for details.
```

### Claude Code
Automatically sees `.code-hq/prompts/` as context.
Ask: "How do I manage tasks in this project?"

## When to Use code-hq

**Always:**
- Check existing tasks before starting work: `code-hq tasks --status todo`
- Update status when starting work: `code-hq update task:123 --status in-progress`
- Record time spent: `code-hq update task:123 --actual-hours 3`
- Document decisions: `code-hq create note "Why we chose X" --note-type decision`

**When Blocked:**
- Update status: `code-hq update task:123 --status blocked`
- Create blocker note: `code-hq create note "Blocked: Waiting for API keys" --related-to task:123`

**When Completed:**
- Mark done: `code-hq update task:123 --status done`
- Validate graph: `code-hq validate`

## Philosophy

**Structured over Unstructured**: Instead of parsing TODO comments, use semantic tasks.

**Query, Don't Parse**: Use `code-hq query` instead of grepping through files.

**Link Everything**: Tasks → Notes → People → Files. Create relationships.

**Update in Real-Time**: Don't let task status drift from reality.

---

**Next**: Read [task-management.md](./task-management.md) for detailed commands.
