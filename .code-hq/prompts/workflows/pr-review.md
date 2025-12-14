# PR Review Workflow

**For AI Agents**: Review pull requests against project tasks and context.

## Purpose

Use code-hq context to:
- Verify PR links to a task
- Check task requirements are met
- Ensure no breaking changes to blocked/in-progress tasks
- Update task status after merge

## Pre-Review Checklist

### 1. Find related task
```bash
# Search for task by PR title or branch name
code-hq query "FIND task WHERE ?t.title CONTAINS '${PR_TITLE}'" --nl

# Or by task ID in PR description
# Parse task:123 from PR body, then:
code-hq query "FIND task WHERE ?t.id = 'task:${TASK_ID}'"
```

### 2. Check task status
```bash
# Verify task is in-progress (not todo or done)
code-hq query "FIND task WHERE ?t.id = 'task:${TASK_ID}' RETURN ?t.status"

# Expected: in-progress or review
```

### 3. Check dependencies
```bash
# Find tasks this PR might affect
code-hq query "FIND task AS ?t 
  WHERE ?t.dependsOn = 'task:${TASK_ID}' 
  OR ?t.blockedBy = 'task:${TASK_ID}' 
  RETURN ?t.title, ?t.status"
```

### 4. Check related notes
```bash
# Find decision notes related to this task
code-hq query "FIND note AS ?n 
  WHERE 'task:${TASK_ID}' IN ?n.relatedTo 
  AND ?n.type = 'decision' 
  RETURN ?n"
```

## Review Guidelines

### Check for Breaking Changes

**If task has dependents:**
```bash
# Find tasks depending on this one
code-hq query "FIND task WHERE ?t.dependsOn = 'task:${TASK_ID}'"

# Review: Will this PR break those tasks?
```

**If task is blocking others:**
```bash
# Find tasks blocked by this one
code-hq query "FIND task WHERE ?t.blockedBy = 'task:${TASK_ID}'"

# After merge: Unblock those tasks
```

### Verify Requirements

**Check task description:**
```bash
code-hq query "FIND task WHERE ?t.id = 'task:${TASK_ID}' RETURN ?t.description"

# Does PR fulfill the description?
```

**Check acceptance criteria:**
Look for related notes with acceptance criteria:
```bash
code-hq query "FIND note 
  WHERE 'task:${TASK_ID}' IN ?n.relatedTo 
  AND 'acceptance-criteria' IN ?n.tags"
```

### Check Time Estimate

**Compare estimated vs actual:**
```bash
code-hq query "FIND task WHERE ?t.id = 'task:${TASK_ID}' 
  RETURN ?t.estimatedHours, ?t.actualHours"

# Flag if actualHours >> estimatedHours (scope creep)
```

## Post-Review Actions

### If Approved & Merged

**1. Update task status:**
```bash
code-hq update task:${TASK_ID} --status done
```

**2. Record actual time:**
```bash
# If PR shows time spent
code-hq update task:${TASK_ID} --actual-hours ${HOURS}
```

**3. Unblock dependent tasks:**
```bash
# For each task that was blocked by this:
code-hq query "FIND task WHERE ?t.blockedBy = 'task:${TASK_ID}'"

# Remove blocker (manual for now)
# Future: code-hq unblock task:456 --from task:123
```

**4. Create completion note:**
```bash
code-hq create note "Completed: ${TASK_TITLE}" \
  --note-type general \
  --content "PR #${PR_NUMBER} merged. Changes: ..." \
  --related-to task:${TASK_ID}
```

### If Changes Requested

**1. Update task with feedback:**
```bash
code-hq create note "PR Review Feedback" \
  --note-type general \
  --content "${REVIEW_COMMENTS}" \
  --related-to task:${TASK_ID} \
  --tags "pr-review,feedback"
```

**2. Keep status as in-progress or review:**
```bash
code-hq update task:${TASK_ID} --status review
```

## Example Agent Workflow

```bash
# Agent receives PR review request
PR_TITLE="Fix authentication bug"
PR_NUMBER=123

# 1. Find related task
TASK_ID=$(code-hq query "FIND task WHERE ?t.title CONTAINS 'authentication bug'" --format json | jq -r '.[0].id')

# 2. Get task context
code-hq query "FIND task WHERE ?t.id = '${TASK_ID}' RETURN ?t"

# 3. Check dependencies
code-hq query "FIND task WHERE ?t.dependsOn = '${TASK_ID}' OR ?t.blockedBy = '${TASK_ID}'"

# 4. Review code changes (use IDE's diff tools)
# ...

# 5. If approved:
code-hq update ${TASK_ID} --status done --actual-hours 4
code-hq create note "Completed via PR #${PR_NUMBER}" --related-to ${TASK_ID}

# 6. Notify dependent tasks
code-hq query "FIND task WHERE ?t.blockedBy = '${TASK_ID}'" --format json | \
  jq -r '.[].id' | \
  while read dep_task; do
    echo "✓ Task ${dep_task} is now unblocked"
  done
```

## Integration with Git Hooks

Add to `.git/hooks/pre-push`:

```bash
#!/bin/bash
# Validate PR has linked task

BRANCH=$(git rev-parse --abbrev-ref HEAD)

# Extract task ID from branch name (e.g., feature/task-123-auth-bug)
TASK_ID=$(echo $BRANCH | grep -oP 'task-\d+')

if [ -z "$TASK_ID" ]; then
  echo "❌ Branch name must include task ID (e.g., feature/task-123-description)"
  exit 1
fi

# Verify task exists
if ! code-hq query "FIND task WHERE ?t.id = '${TASK_ID}'" --format json | jq -e '.[0]' > /dev/null; then
  echo "❌ Task ${TASK_ID} not found in code-hq"
  exit 1
fi

# Verify task is in-progress
STATUS=$(code-hq query "FIND task WHERE ?t.id = '${TASK_ID}' RETURN ?t.status" --format json | jq -r '.[0].status')
if [ "$STATUS" != "in-progress" ] && [ "$STATUS" != "review" ]; then
  echo "⚠️  Warning: Task ${TASK_ID} status is ${STATUS} (expected in-progress or review)"
fi

echo "✅ Task ${TASK_ID} linked to PR"
```

## Tips for Agents

- Always link PRs to tasks - no orphan PRs
- Check for breaking changes by querying dependents
- Update task status immediately after merge
- Document significant changes in notes
- Flag scope creep (actual >> estimated hours)
- Unblock dependent tasks after completion

---

**Next**: See [sprint-planning.md](./sprint-planning.md) for sprint workflows.
