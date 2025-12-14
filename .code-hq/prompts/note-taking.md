# Note Taking with code-hq

**For AI Agents**: Document decisions, meetings, research, and ideas in a structured way.

## Note Types

code-hq supports 5 types of notes:

- **decision** - Architecture decisions, trade-offs, why we chose X over Y
- **meeting** - Meeting notes, discussions, action items
- **research** - Investigation results, benchmarks, findings
- **idea** - Feature ideas, improvements, brainstorming
- **general** - Everything else

## Create Notes

### Basic Note
```bash
code-hq create note "Title of note" \
  --note-type decision \
  --content "Content goes here..."
```

### With Tags
```bash
code-hq create note "API Design Decision" \
  --note-type decision \
  --content "We chose REST over GraphQL because..." \
  --tags "api,architecture,backend"
```

### Linked to Task
```bash
code-hq create note "Blocker: Waiting for API keys" \
  --note-type general \
  --content "Need production OAuth keys from ops team" \
  --related-to task:123
```

## List Notes

```bash
# All notes
code-hq notes

# Filter by type
code-hq notes --type decision
code-hq notes --type meeting

# Filter by tag
code-hq notes --tag architecture
```

## Query Notes

### Find decision notes
```bash
code-hq query "FIND note AS ?n WHERE ?n.type = 'decision' RETURN ?n"
```

### Find notes about specific topic
```bash
code-hq query "FIND note AS ?n WHERE 'authentication' IN ?n.tags RETURN ?n"
```

### Find notes related to task
```bash
code-hq query "FIND note AS ?n WHERE 'task:123' IN ?n.relatedTo RETURN ?n"
```

### Find recent notes
```bash
code-hq query "FIND note AS ?n 
  WHERE ?n.createdAt > '2025-01-15' 
  RETURN ?n.title, ?n.type, ?n.createdAt"
```

## Use Cases

### 1. Document Architecture Decisions

**When making significant technical decisions:**

```bash
code-hq create note "Use PostgreSQL for primary database" \
  --note-type decision \
  --content "
Decision: Use PostgreSQL over MongoDB

Context: Need ACID transactions and complex queries

Considered:
- PostgreSQL: ACID, mature, excellent query capabilities
- MongoDB: Flexible schema, but weak transactions
- MySQL: Good, but PostgreSQL has better JSON support

Decision: PostgreSQL

Consequences:
- Need to define strict schemas
- Better data integrity
- Easier complex queries
" \
  --tags "database,architecture,backend"
```

### 2. Meeting Notes

**After team meetings:**

```bash
code-hq create note "Sprint Planning - Jan 20" \
  --note-type meeting \
  --content "
Attendees: Alice, Bob, Charlie

Agenda:
1. Review last sprint
2. Plan next sprint
3. Discuss blockers

Decisions:
- Focus on auth features next sprint
- Bob to lead OAuth implementation
- Charlie to handle 2FA

Action Items:
- Alice: Create tasks for auth features
- Bob: Research OAuth providers
- Charlie: Draft 2FA spec
" \
  --tags "sprint-planning,meeting"
```

### 3. Research Findings

**After investigating options:**

```bash
code-hq create note "OAuth Provider Comparison" \
  --note-type research \
  --content "
Compared: Auth0, Okta, AWS Cognito, Firebase Auth

Criteria: Cost, features, ease of integration, scalability

Results:
- Auth0: $$$, best features, easy, scales well
- Okta: $$$$$, enterprise-focused, complex
- AWS Cognito: $$, good AWS integration, quirky API
- Firebase: $, simple, limited customization

Recommendation: Auth0 for MVP, consider AWS Cognito later
" \
  --tags "research,oauth,auth" \
  --related-to task:auth-123
```

### 4. Feature Ideas

**When brainstorming:**

```bash
code-hq create note "Idea: Real-time collaboration" \
  --note-type idea \
  --content "
What if multiple team members could edit the knowledge graph simultaneously?

Features:
- WebSocket connections
- Operational transforms (OT) or CRDTs
- Presence indicators
- Conflict resolution

Challenges:
- Complex implementation
- Need backend service
- Potential conflicts

Next Steps:
- Research CRDT libraries
- Estimate effort
- Validate with users
" \
  --tags "feature-idea,collaboration"
```

### 5. Blocker Documentation

**When tasks are blocked:**

```bash
# First, mark task as blocked
code-hq update task:123 --status blocked

# Then document why
code-hq create note "Blocker: OAuth keys needed" \
  --note-type general \
  --content "
Task task:123 is blocked waiting for production OAuth credentials.

Requested from: ops@company.com
Request date: Jan 20, 2025
Expected: Jan 25, 2025

Impact:
- Cannot test OAuth flow in production
- Cannot complete auth feature
- Blocking 3 other tasks (task:456, task:789, task:012)
" \
  --related-to task:123 \
  --tags "blocker,oauth"
```

## Best Practices for Agents

### When to Create Notes

**Always:**
- After making architecture decisions
- When discovering important information
- When encountering blockers
- When concluding research
- After meetings with action items

**Consider:**
- When fixing non-obvious bugs (document root cause)
- When deviating from plan (document why)
- When finding better approaches (document for future)

### Note Content Guidelines

**Be specific:**
- ❌ "We chose option A"
- ✅ "We chose PostgreSQL over MongoDB because we need ACID transactions"

**Include context:**
- Why was this decision needed?
- What options were considered?
- What were the trade-offs?

**Link related entities:**
```bash
--related-to task:123,task:456,milestone:v1.0
```

**Use consistent tags:**
- Technical: `backend`, `frontend`, `database`, `api`
- Process: `architecture`, `decision`, `blocker`
- Domain: `auth`, `billing`, `analytics`

### Searching Notes

**Before creating a decision note:**
```bash
# Check if similar decision was already made
code-hq query "FIND note WHERE ?n.type = 'decision' AND 'database' IN ?n.tags"
```

**Before researching:**
```bash
# Check if someone already researched this
code-hq query "FIND note WHERE ?n.type = 'research' AND 'oauth' IN ?n.tags"
```

## Integration with Tasks

### Link notes to tasks
```bash
# When creating note
code-hq create note "..." --related-to task:123

# Query to find all notes for a task
code-hq query "FIND note WHERE 'task:123' IN ?n.relatedTo"
```

### Create task from note
```bash
# If note describes work to be done
code-hq create task "Implement OAuth" \
  --description "See note:oauth-research-456 for details" \
  --related-note note:oauth-research-456
```

## Templates

### Decision Template
```markdown
Decision: [What was decided]

Context: [Why was this decision needed]

Options Considered:
- Option A: [pros/cons]
- Option B: [pros/cons]

Decision: [Which option and why]

Consequences:
- [Impact 1]
- [Impact 2]

Reviewed by: [Who reviewed this]
Date: [When]
```

### Meeting Template
```markdown
Meeting: [Topic]
Date: [Date]
Attendees: [Names]

Agenda:
1. [Item 1]
2. [Item 2]

Discussion:
- [Key points]

Decisions:
- [Decision 1]
- [Decision 2]

Action Items:
- [ ] [Action 1] (@person)
- [ ] [Action 2] (@person)

Next Meeting: [Date/topic]
```

### Research Template
```markdown
Research: [What was investigated]

Goal: [What were you trying to learn]

Methodology:
- [How you researched]

Findings:
- [Key finding 1]
- [Key finding 2]

Comparison:
| Option | Pro | Con | Score |
|--------|-----|-----|-------|
| A      | ... | ... | 8/10  |
| B      | ... | ... | 6/10  |

Recommendation: [What should we do]

References:
- [Source 1]
- [Source 2]
```

## Tips for Agents

1. **Create notes liberally** - Better over-documented than under
2. **Tag consistently** - Makes searching easier
3. **Link to tasks** - Provides context for future work
4. **Use the right type** - Helps filter and organize
5. **Write for future you** - Someone will read this in 6 months
6. **Include dates** - When was this true? Context changes over time
7. **Link sources** - Where did this information come from?

## Common Queries

```bash
# All decisions
code-hq notes --type decision

# Recent research
code-hq query "FIND note WHERE ?n.type = 'research' AND ?n.createdAt > '2025-01-01'"

# Meeting notes with action items
code-hq query "FIND note WHERE ?n.type = 'meeting' AND ?n.content CONTAINS 'action'"

# Notes by tag
code-hq notes --tag architecture

# Notes related to milestone
code-hq query "FIND note WHERE 'milestone:v1.0' IN ?n.relatedTo"
```

---

**Next**: See [query-examples.md](./query-examples.md) for advanced queries.
