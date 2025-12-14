# Task Management with code-hq

**For AI Agents**: Complete guide to managing project tasks with code-hq.

## Quick Reference

```bash
# List tasks
code-hq tasks                           # All tasks
code-hq tasks --status blocked          # Filter by status
code-hq tasks --assignee @me            # My tasks
code-hq tasks --priority high           # Filter by priority

# Create task
code-hq create task "Title" \
  --priority high \
  --status in-progress \
  --assignee person:alice \
  --tags "auth,bug" \
  --due "2025-01-20"

# Update task
code-hq update task:123 --status done
code-hq update task:123 --actual-hours 4
code-hq update task:123 --add-tags "urgent"

# Query tasks
code-hq query "FIND task WHERE ?t.status = 'blocked' RETURN ?t"
```

## Task Lifecycle

```
todo → in-progress → review → done
          ↓
       blocked (temporary)
```

## Available Commands

### List Tasks

**All tasks:**
```bash
code-hq tasks
```

**Filter by status:**
```bash
code-hq tasks --status todo
code-hq tasks --status in-progress
code-hq tasks --status blocked
code-hq tasks --status review
code-hq tasks --status done
```

**Filter by assignee:**
```bash
code-hq tasks --assignee person:alice
code-hq tasks --assignee @me  # Current user
```

**Filter by priority:**
```bash
code-hq tasks --priority low
code-hq tasks --priority medium
code-hq tasks --priority high
code-hq tasks --priority critical
```

**Filter by tag:**
```bash
code-hq tasks --tag security
code-hq tasks --tag bug
```

**Combine filters:**
```bash
code-hq tasks --status blocked --assignee @me --priority high
```

### Create Tasks

**Basic task:**
```bash
code-hq create task "Fix authentication bug"
```

**With all options:**
```bash
code-hq create task "Implement OAuth flow" \
  --priority high \
  --status in-progress \
  --assignee person:alice \
  --tags "auth,security,backend" \
  --due "2025-01-20T15:00:00Z"
```

**Task fields:**
- `title` (required) - What needs to be done
- `priority` - `low`, `medium`, `high`, `critical` (default: medium)
- `status` - `todo`, `in-progress`, `blocked`, `review`, `done` (default: todo)
- `assignee` - Who's responsible (format: `person:name`)
- `tags` - Comma-separated labels
- `due` - Due date (ISO 8601 format)

### Update Tasks

**Change status:**
```bash
code-hq update task:123 --status in-progress
code-hq update task:123 --status done
code-hq update task:123 --status blocked
```

**Change priority:**
```bash
code-hq update task:123 --priority critical
```

**Reassign:**
```bash
code-hq update task:123 --assignee person:bob
```

**Add tags:**
```bash
code-hq update task:123 --add-tags "urgent,hotfix"
```

**Track time:**
```bash
code-hq update task:123 --actual-hours 4
```

**Multiple updates at once:**
```bash
code-hq update task:123 \
  --status done \
  --actual-hours 6 \
  --add-tags "completed"
```

### Query Tasks

**Find blocked tasks:**
```bash
code-hq query "FIND task AS ?t WHERE ?t.status = 'blocked' RETURN ?t"
```

**Find my tasks:**
```bash
code-hq query "FIND task AS ?t WHERE ?t.assignee = '@me' RETURN ?t"
```

**Find overdue tasks:**
```bash
code-hq query "FIND task AS ?t 
  WHERE ?t.dueDate < today 
  AND ?t.status != 'done' 
  RETURN ?t"
```

**Find high priority unassigned tasks:**
```bash
code-hq query "FIND task AS ?t 
  WHERE ?t.priority = 'high' 
  AND ?t.assignee = null 
  RETURN ?t"
```

See [query-examples.md](./query-examples.md) for more query patterns.

## Common Workflows

### Starting Work on a Task

**1. Find available tasks:**
```bash
# See your assigned tasks
code-hq tasks --assignee @me --status todo

# Or find unassigned high priority tasks
code-hq tasks --priority high --status todo
```

**2. Update status:**
```bash
code-hq update task:123 --status in-progress
```

**3. (Optional) Add context:**
```bash
# If you discover more information
code-hq create note "Implementation notes for task:123" \
  --content "Need to refactor auth module first" \
  --related-to task:123
```

### Getting Blocked

**1. Update task status:**
```bash
code-hq update task:123 --status blocked
```

**2. Document the blocker:**
```bash
code-hq create note "Blocker: Waiting for API keys" \
  --content "Cannot test OAuth flow without production keys. 
  Requested from ops@company.com on Jan 20.
  Expected: Jan 25." \
  --related-to task:123 \
  --tags "blocker,oauth"
```

**3. (Optional) Link to blocking task:**
If another task is blocking this one:
```bash
# For now, document in task description
# Future: code-hq link task:123 --blocked-by task:456
```

### Completing a Task

**1. Update status:**
```bash
code-hq update task:123 --status done
```

**2. Record time spent:**
```bash
code-hq update task:123 --actual-hours 6
```

**3. (Optional) Create completion note:**
```bash
code-hq create note "Completed: OAuth implementation" \
  --content "Implemented OAuth 2.0 flow using Auth0.
  Files: src/auth/oauth.ts, src/routes/auth.ts
  Tests: test/auth/oauth.test.ts
  PR: #456" \
  --related-to task:123
```

**4. Validate:**
```bash
code-hq validate
```

### Reviewing Code

**1. Find task by PR/branch:**
```bash
# If branch name includes task ID
code-hq query "FIND task WHERE ?t.id = 'task:123'"

# Or search by title
code-hq query "FIND task WHERE ?t.title CONTAINS 'authentication'"
```

**2. Check task requirements:**
```bash
# View task details
code-hq query "FIND task WHERE ?t.id = 'task:123' RETURN ?t.description"

# Check related notes
code-hq query "FIND note WHERE 'task:123' IN ?n.relatedTo"
```

**3. After approval:**
```bash
code-hq update task:123 --status done
```

See [workflows/pr-review.md](./workflows/pr-review.md) for detailed PR workflow.

### Sprint Planning

**1. List unassigned tasks:**
```bash
code-hq query "FIND task WHERE ?t.assignee = null AND ?t.status = 'todo'"
```

**2. Check workload:**
```bash
code-hq query "FIND task AS ?t 
  WHERE ?t.status IN ('todo', 'in-progress') 
  RETURN ?t.assignee, COUNT(?t) 
  GROUP BY ?t.assignee"
```

**3. Assign tasks:**
```bash
code-hq update task:123 --assignee person:alice
code-hq update task:456 --assignee person:bob
```

**4. Set priorities:**
```bash
code-hq update task:123 --priority high
code-hq update task:456 --priority medium
```

## Best Practices

### Before Creating Tasks

**1. Check for duplicates:**
```bash
# Search by title keywords
code-hq query "FIND task WHERE ?t.title CONTAINS 'authentication'"

# Or list all tasks
code-hq tasks
```

**2. Use descriptive titles:**
- ❌ "Fix bug"
- ✅ "Fix authentication timeout in OAuth flow"

**3. Add context:**
```bash
code-hq create task "Fix OAuth timeout" \
  --priority high \
  --tags "auth,bug,timeout" \
  --description "Users getting timeouts after 5 seconds..."
```

### While Working

**1. Update status immediately:**
```bash
# When you start
code-hq update task:123 --status in-progress

# When you finish
code-hq update task:123 --status done
```

**2. Track time accurately:**
```bash
# Add time as you work (or at end of day)
code-hq update task:123 --actual-hours 2
```

**3. Document blockers promptly:**
```bash
# Don't let blockers linger undocumented
code-hq update task:123 --status blocked
code-hq create note "Blocker: ..." --related-to task:123
```

### Task Organization

**1. Use consistent tags:**
- Technical: `frontend`, `backend`, `database`, `api`
- Type: `bug`, `feature`, `refactor`, `docs`
- Domain: `auth`, `billing`, `analytics`
- Status: `hotfix`, `urgent`, `nice-to-have`

**2. Set realistic priorities:**
- `critical` - Production down, data loss risk
- `high` - Blocking other work, user-facing bug
- `medium` - Normal feature work
- `low` - Nice-to-have, technical debt

**3. Keep tasks focused:**
- One task = One PR (ideally)
- If task is too big, break it down
- Use dependencies to link related tasks

## Status Meanings

- **todo** - Ready to be worked on, not started
- **in-progress** - Actively being worked on
- **blocked** - Can't proceed (waiting on something)
- **review** - Code written, needs review
- **done** - Complete and merged/deployed

## Task Dependencies (Coming Soon)

Future syntax (not yet implemented):
```bash
# Make task:456 depend on task:123
code-hq link task:456 --depends-on task:123

# Mark task as blocking another
code-hq link task:123 --blocks task:456

# Find dependencies
code-hq query "FIND task WHERE ?t.dependsOn = 'task:123'"
```

For now: Document dependencies in task descriptions or notes.

## Integration with Git

### Branch Naming
```bash
# Include task ID in branch name
git checkout -b feature/task-123-oauth-implementation
git checkout -b fix/task-456-timeout-bug
```

### Commit Messages
```bash
# Reference task in commits
git commit -m "feat: implement OAuth flow (task:123)"
git commit -m "fix: resolve timeout issue (task:456)"
```

### PR Descriptions
```markdown
# PR Template
## Related Task
task:123

## Changes
- Implemented OAuth 2.0 flow
- Added tests
- Updated docs

## Testing
- Manual testing with Auth0
- Unit tests pass
- Integration tests pass
```

## Useful Views

**Kanban board:**
```bash
code-hq show --view kanban
```

**Calendar view:**
```bash
code-hq show --view calendar
```

**Data table:**
```bash
code-hq show  # Default view
```

## Tips for Agents

1. **Always check existing tasks first** before creating new ones
2. **Update status in real-time** - don't let it drift
3. **Link everything** - tasks to notes, notes to tasks
4. **Query before parsing** - use `code-hq query` instead of grepping
5. **Track time consistently** - helps with estimation
6. **Document blockers immediately** - visibility is key
7. **Use tags liberally** - makes filtering easier
8. **Validate after bulk changes** - `code-hq validate`

## Common Queries

See [query-examples.md](./query-examples.md) for comprehensive query patterns.

Quick examples:
```bash
# My work today
code-hq tasks --assignee @me --status in-progress

# Blocked tasks
code-hq tasks --status blocked

# Overdue tasks
code-hq query "FIND task WHERE ?t.dueDate < today AND ?t.status != 'done'"

# Unassigned high priority
code-hq tasks --priority high --status todo | grep "Assignee: $"

# Tasks by tag
code-hq tasks --tag security
```

## Troubleshooting

**Task not found:**
```bash
# List all task IDs
code-hq tasks --format json | jq '.[].id'

# Validate graph
code-hq validate
```

**Query fails:**
```bash
# Check available fields
code-hq query --catalog

# Try natural language
code-hq query "show blocked tasks" --nl
```

**Status not updating:**
```bash
# Verify with query
code-hq query "FIND task WHERE ?t.id = 'task:123' RETURN ?t.status"

# Check validation
code-hq validate
```

---

**Next Steps:**
- Read [note-taking.md](./note-taking.md) for documentation patterns
- Read [query-examples.md](./query-examples.md) for advanced queries
- Read [workflows/](./workflows/) for automation patterns
