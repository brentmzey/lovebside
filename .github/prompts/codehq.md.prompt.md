---
mode: agent
description: code-hq commands reference and workflow overview
---

---


Use this workflow to access code-hq commands and understand available workflows for project management.

## Available Workflows

- **/hq-debug** - Systematic diagnosis and resolution of bugs
- **/hq-ux** - UI/UX design and accessibility improvements  
- **/hq-implement** - Feature implementation and development
- **/timesheet** - Generate timesheets and time reports

## Quick Reference

### Task Management
```bash
code-hq tasks                    # All tasks
code-hq tasks --status blocked   # Filtered tasks
code-hq create task "Title" --priority high --estimated-hours 1
code-hq update task:123 --status done
code-hq update task:123 --estimated-hours 2
```

### Time Tracking
```bash
code-hq start task:123 --description "Working on feature"
code-hq stop task:123            # Stop timer and log hours
code-hq time task:123            # View time details
code-hq status                   # Show all active timers
code-hq tasks --active           # Tasks with running timers
```

### Timesheet Reports
```bash
code-hq timesheet --from 7d      # Last 7 days (table format)
code-hq timesheet --from 1m --format csv --output timesheet.csv
code-hq timesheet --project ACME --assignee @alice
```

### Note Taking
```bash
code-hq notes                    # All notes
code-hq create note "Title" --note-type decision
code-hq create note "Title" --content "..."
```

### Query System
```bash
code-hq query "FIND task WHERE ?t.status = 'blocked' RETURN ?t"
```

### Validation
```bash
code-hq validate                 # Check graph integrity
```

## Integration with VSCode

The code-hq extension provides:
- Visual task boards (Kanban, Timeline, Calendar, Table)
- File-based note storage in `.code-hq/notes/`
- Workflow-specific note templates
- Task timing and tracking
- Real-time synchronization

## When to Use code-hq

**Always:**
- Check existing tasks before starting work
- Update status when starting/finishing work
- Record time spent on tasks
- Document decisions and research

**When Blocked:**
- Update task status to 'blocked'
- Create blocker notes with related tasks

**When Completed:**
- Mark tasks as 'done'
- Validate the knowledge graph

## File Structure

- `.code-hq/graph.jsonld` - Main knowledge graph
- `.code-hq/notes/` - File-based notes (Markdown with YAML)
- `.windsurf/workflows/` - Workflow definitions for slash commands

## Best Practices

- Use semantic tasks over TODO comments
- Query instead of parsing files
- Link related entities (tasks → notes → people)
- Update status in real-time
- Document decisions and rationale

---

For detailed guides, see `.code-hq/prompts/` directory..