# B-Side Code-HQ Knowledge Graph

This directory contains the project knowledge graph managed by [code-hq](https://github.com/trentbrew/code-hq).

## 📁 Structure

```
.code-hq/
├── README.md                    # This file
├── graph.jsonld                 # **Primary source of truth** - All entities in JSON-LD
├── PROJECT_STATUS.md            # Human-readable status reports
├── KANBAN.md                    # Kanban board export
├── TEST_SUMMARY.md              # Test documentation
├── JIRA_IMPORT.csv              # CSV for external tools
│
├── notes/                       # Project notes
├── prompts/                     # AI agent prompts
├── workflows/                   # Automation workflows
├── schema/                      # Custom entity schemas
└── views/                       # Custom dashboards
```

**Important**: All tasks, milestones, and people are stored in `graph.jsonld`. Use code-hq commands to interact with the graph.

## 🎯 Quick Commands

```bash
# View all tasks
npx code-hq tasks

# Show kanban board
npx code-hq show --view kanban

# Create a new task
npx code-hq create task "Task name" --priority high --status in-progress

# Start time tracking
npx code-hq start task:msg-001

# Stop and log time
npx code-hq stop task:msg-001

# Generate timesheet
npx code-hq timesheet --from 7d

# Validate graph
npx code-hq validate

# Query with EQL-S
npx code-hq query 'FIND Task WHERE status = "in-progress"'

# Natural language query
npx code-hq query "all high priority tasks for milestone m3" --nl
```

## 📊 Current Sprint Summary

**Sprint 1 - Messaging Foundation**
- ✅ Backend Infrastructure (100%)
- ✅ SDK Integration (100%)
- ✅ Testing (100%)
- 🏗️ UI Development (40%)

**Graph Status:**
- **65 tasks** across 6 milestones
- **Validation**: ✅ Valid (0 errors, 44 internal dependency warnings)
- **Active Milestone**: M3 - Messaging Beta (Apr 15, 2026)

## 📋 Milestones

| ID | Title | Due Date | Status |
|----|-------|----------|--------|
| `milestone:m1` | Navigation Complete | Jan 15, 2026 | In Progress |
| `milestone:m2` | Core MVP | Feb 28, 2026 | To Do |
| `milestone:m3` | Messaging Beta | Apr 15, 2026 | To Do |
| `milestone:m4` | Polish Release | May 31, 2026 | To Do |
| `milestone:m5` | Beta Launch | Jun 30, 2026 | To Do |
| `milestone:m6` | Public Launch | Jul 31, 2026 | To Do |

## 🏷️ Task Categories

- `task:nav-*` - Navigation & routing
- `task:auth-*` - Authentication flows
- `task:msg-*` - Messaging features
- `task:match-*` - Discovery & matching
- `task:profile-*` - Profile management
- `task:proust-*` - Questionnaire system
- `task:graph-*` - Emotion graph features
- `task:polish-*` - UI polish & animations
- `task:beta-*` - Beta preparation
- `task:launch-*` - Launch readiness

## 🔄 Workflow with Code-HQ

### Working on Tasks

```bash
# 1. Find a task
npx code-hq tasks --status todo --priority high

# 2. Start working (begins time tracking)
npx code-hq start task:msg-003

# 3. Update status as you progress
npx code-hq update task:msg-003 --status in-progress

# 4. Stop tracking when done
npx code-hq stop task:msg-003

# 5. Mark complete
npx code-hq update task:msg-003 --status done
```

### Creating New Work

```bash
# Create a task
npx code-hq create task "Implement feature X" \
  --priority high \
  --status todo \
  --estimated-hours 8 \
  --tags feature,ui \
  --milestone milestone:m3

# Create a note
npx code-hq create note "Architecture Decision: Using SSE for real-time" \
  --note-type decision

# Create a milestone
npx code-hq create milestone "Phase 4 Complete" \
  --due-date 2026-08-31
```

### Reporting

```bash
# Daily standup
npx code-hq tasks --status in-progress
npx code-hq timesheet --from 1d

# Weekly review
npx code-hq timesheet --from 7d
npx code-hq tasks --status done --from 7d

# Sprint planning
npx code-hq show --view kanban
npx code-hq tasks --milestone milestone:m3 --status todo
```

## 🔗 Integration with GitHub

### Commit Messages
Reference tasks in commits:
```bash
git commit -m "task:msg-003: Implement push notification infrastructure"
```

### Pull Requests
Link to code-hq tasks:
```markdown
## Related Tasks
- task:msg-003 (Push Notifications)
- Closes task:msg-003

## Changes
- Added FCM integration
- Created notification service
```

## 📈 Advanced Usage

### EQL-S Queries

```bash
# Find all high-priority tasks
npx code-hq query 'FIND Task WHERE priority = "high"'

# Find tasks for a specific milestone
npx code-hq query 'FIND Task WHERE targetMilestone.@id = "milestone:m3"'

# Find tasks by tag
npx code-hq query 'FIND Task WHERE "messaging" IN tags'

# Count tasks by status
npx code-hq query 'FIND Task GROUP BY status'
```

### Natural Language Queries

```bash
# Let AI interpret your query
npx code-hq query "show me all messaging tasks" --nl
npx code-hq query "what are the blockers?" --nl
npx code-hq query "time spent on milestone 3" --nl
```

## 📚 Resources

- **code-hq**: https://github.com/trentbrew/code-hq
- **TQL/EQL-S**: https://github.com/trentbrew/TQL
- **JSON-LD**: https://json-ld.org/
- **Project Status**: [PROJECT_STATUS.md](PROJECT_STATUS.md)
- **Test Coverage**: [TEST_SUMMARY.md](TEST_SUMMARY.md)

## 🆘 Troubleshooting

### Graph validation warnings
The 44 dependency warnings are expected (code-hq internal validation quirk). As long as errors = 0, you're good.

### Missing entities
All entities must be in `graph.jsonld`. Don't create Markdown files in `entities/` folder.

### Time tracking not working
Make sure you `start` a task before `stop`. Check `npx code-hq timesheet` to see logged time.
