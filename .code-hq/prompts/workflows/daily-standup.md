# Daily Standup Workflow

**For AI Agents**: Generate a daily standup report for a team member.

## Purpose

Automatically generate a standup report showing:
- What did I work on yesterday?
- What am I working on today?
- What's blocking me?

## Steps

### 1. Get yesterday's tasks
```bash
# Find tasks updated yesterday that are assigned to me
code-hq query "FIND task AS ?t 
  WHERE ?t.assignee = '@me' 
  AND ?t.updatedAt > 'yesterday' 
  RETURN ?t.title, ?t.status, ?t.actualHours"
```

### 2. Get tasks in progress
```bash
# Find my current tasks
code-hq tasks --assignee @me --status in-progress
```

### 3. Get blockers
```bash
# Find my blocked tasks
code-hq tasks --assignee @me --status blocked
```

### 4. Get today's planned work
```bash
# Find my todo tasks
code-hq tasks --assignee @me --status todo --priority high
```

## Example Output

```markdown
# Standup for Alice - Jan 20, 2025

## Yesterday
- ✅ Completed: Fix authentication bug (task:auth-123) - 4 hours
- 🔄 Continued: Implement OAuth flow (task:oauth-456) - 2 hours

## Today
- Continue: Implement OAuth flow (task:oauth-456)
- Start: Add 2FA support (task:2fa-789)

## Blockers
- Need API keys for OAuth testing (task:oauth-456)
- Waiting on database schema approval (task:db-schema-012)

## Time Yesterday: 6 hours
## Planned Today: 8 hours
```

## Automation

You can automate this with a shell script:

```bash
#!/bin/bash
# .code-hq/scripts/standup.sh

echo "# Standup for $(git config user.name) - $(date +%Y-%m-%d)"
echo ""

echo "## Yesterday"
code-hq query "FIND task WHERE ?t.assignee = '@me' AND ?t.updatedAt > 'yesterday' AND ?t.status = 'done'" --format json | jq -r '.[] | "- ✅ Completed: \(.title) (\(.id)) - \(.actualHours) hours"'

echo ""
echo "## Today"
code-hq tasks --assignee @me --status in-progress --format json | jq -r '.[] | "- 🔄 Continue: \(.title) (\(.id))"'
code-hq tasks --assignee @me --status todo --priority high --format json | jq -r '.[] | "- 🆕 Start: \(.title) (\(.id))"' | head -3

echo ""
echo "## Blockers"
code-hq tasks --assignee @me --status blocked --format json | jq -r '.[] | "- 🚧 \(.title) (\(.id))"'
```

## Agent Instructions

When asked to generate a standup report:

1. **Run the queries above** to gather data
2. **Format the output** as markdown
3. **Include time tracking** if available
4. **Highlight blockers** prominently
5. **Keep it concise** - only relevant tasks

### Common Variations

**For team lead**: Generate standups for all team members
```bash
for person in alice bob charlie; do
  echo "## $person"
  code-hq tasks --assignee person:$person --status in-progress
  echo ""
done
```

**For sprint review**: Show completed tasks this sprint
```bash
code-hq query "FIND task 
  WHERE ?t.milestone = 'milestone:sprint-12' 
  AND ?t.status = 'done' 
  RETURN ?t"
```

**For capacity planning**: Show workload distribution
```bash
code-hq query "FIND task 
  WHERE ?t.status IN ('todo', 'in-progress') 
  RETURN ?t.assignee, COUNT(?t) 
  GROUP BY ?t.assignee"
```

## Tips

- Run this first thing in the morning
- Update task statuses before generating report
- Use `--actual-hours` to track time accurately
- Create blocker notes for visibility: `code-hq create note "Blocker: ..." --related-to task:123`

---

**Next**: See [pr-review.md](./pr-review.md) for PR review workflows.
