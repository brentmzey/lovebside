# code-hq Query Examples

**For AI Agents**: This guide shows common query patterns using code-hq's EQL-S (Entity Query Language).

## Basic Queries

### Find all blocked tasks
```bash
code-hq query "FIND task AS ?t WHERE ?t.status = 'blocked' RETURN ?t"
```

### Find high priority tasks
```bash
code-hq query "FIND task AS ?t WHERE ?t.priority = 'high' RETURN ?t"
```

### Find tasks assigned to specific person
```bash
code-hq query "FIND task AS ?t WHERE ?t.assignee = 'person:alice' RETURN ?t"
```

### Find tasks with specific tag
```bash
code-hq query "FIND task AS ?t WHERE 'security' IN ?t.tags RETURN ?t"
```

## Complex Queries

### Find overdue tasks
```bash
code-hq query "FIND task AS ?t 
  WHERE ?t.dueDate < '2025-01-20' 
  AND ?t.status != 'done' 
  RETURN ?t.title, ?t.dueDate, ?t.assignee"
```

### Find tasks with dependencies
```bash
code-hq query "FIND task AS ?t 
  WHERE ?t.dependsOn = ?dep 
  RETURN ?t.title, ?dep"
```

### Find unassigned high priority tasks
```bash
code-hq query "FIND task AS ?t 
  WHERE ?t.priority = 'high' 
  AND ?t.assignee = null 
  RETURN ?t"
```

### Find tasks by multiple tags
```bash
code-hq query "FIND task AS ?t 
  WHERE 'auth' IN ?t.tags 
  AND 'security' IN ?t.tags 
  RETURN ?t"
```

## Aggregations

### Count tasks by status
```bash
code-hq query "FIND task AS ?t 
  RETURN ?t.status, COUNT(?t) 
  GROUP BY ?t.status"
```

### Find people with task counts
```bash
code-hq query "FIND task AS ?t 
  RETURN ?t.assignee, COUNT(?t) 
  GROUP BY ?t.assignee"
```

### Average actual hours by priority
```bash
code-hq query "FIND task AS ?t 
  WHERE ?t.actualHours != null 
  RETURN ?t.priority, AVG(?t.actualHours) 
  GROUP BY ?t.priority"
```

## Natural Language Queries

Instead of writing EQL-S, use `--nl` flag:

```bash
# These work with --nl flag
code-hq query "show me all blocked tasks" --nl
code-hq query "what tasks is alice working on" --nl
code-hq query "which high priority tasks are overdue" --nl
code-hq query "show tasks without assignees" --nl
```

## Notes Queries

### Find decision notes
```bash
code-hq query "FIND note AS ?n WHERE ?n.type = 'decision' RETURN ?n"
```

### Find notes with specific tag
```bash
code-hq query "FIND note AS ?n WHERE 'architecture' IN ?n.tags RETURN ?n"
```

### Find notes created this week
```bash
code-hq query "FIND note AS ?n 
  WHERE ?n.createdAt > '2025-01-15' 
  RETURN ?n.title, ?n.createdAt"
```

## Cross-Entity Queries

### Find tasks and their related notes
```bash
code-hq query "FIND task AS ?t, note AS ?n 
  WHERE ?t.id IN ?n.relatedTo 
  RETURN ?t.title, ?n.title"
```

### Find person's tasks and time spent
```bash
code-hq query "FIND task AS ?t 
  WHERE ?t.assignee = 'person:alice' 
  RETURN ?t.title, ?t.actualHours"
```

## Useful Patterns for Agents

### Before starting work
Check what you're assigned:
```bash
code-hq tasks --assignee @me --status todo
```

### When investigating blockers
Find all blocked tasks:
```bash
code-hq query "FIND task WHERE ?t.status = 'blocked' RETURN ?t"
```

### Sprint planning
Find unassigned tasks:
```bash
code-hq query "FIND task WHERE ?t.assignee = null AND ?t.status = 'todo'"
```

### Progress tracking
Count completed tasks:
```bash
code-hq query "FIND task WHERE ?t.status = 'done' AND ?t.updatedAt > '2025-01-01' RETURN COUNT(?t)"
```

## Output Formats

```bash
# JSON (for parsing)
code-hq query "FIND task RETURN ?t" --format json

# Table (human-readable)
code-hq query "FIND task RETURN ?t" --format table

# CSV (for export)
code-hq query "FIND task RETURN ?t" --format csv

# Limit results
code-hq query "FIND task RETURN ?t" --limit 10
```

## Tips for Agents

1. **Use filters first**: `code-hq tasks --status blocked` is faster than querying
2. **Cache catalog**: Run `code-hq query --catalog` once to understand available fields
3. **Start specific**: Query for what you need, not everything
4. **Use natural language**: When EQL-S is unclear, use `--nl` flag
5. **Validate after**: Run `code-hq validate` after bulk updates

## Error Handling

If query fails:
1. Check syntax: EQL-S is case-sensitive
2. Verify field names: Use `code-hq query --catalog` to see available fields
3. Check entity types: Must be `task`, `note`, `person`, or `milestone`
4. Try natural language: `--nl` flag often works when syntax is wrong

---

**Pro Tip**: For complex queries, ask the AI to generate the EQL-S syntax first, then run it.
