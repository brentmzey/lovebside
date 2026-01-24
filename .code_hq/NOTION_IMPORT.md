# BSide Project - Notion Import Guide

**Last Updated:** 2026-01-24  
**Format:** Markdown (Notion-compatible)  
**Structure:** Ready for Notion import

---

## 📥 How to Import to Notion

### Method 1: Bulk Import (Recommended)
1. Open Notion workspace
2. Click "Import" in sidebar
3. Select "Markdown & CSV"
4. Choose `.code_hq` directory
5. Notion will create pages automatically

### Method 2: Individual Pages
1. Create new page in Notion
2. Type `/import`
3. Select "Markdown"
4. Choose specific `.md` file

---

## 📁 File Structure for Import

```
.code_hq/
├── PROJECT_MANAGEMENT.md      → "📊 Project Management" page
├── boards/
│   ├── KANBAN.md             → "🎯 Kanban Board" page
│   └── SCRUM.md              → "🏃 Scrum Board" page
├── epics/
│   └── EPICS_OVERVIEW.md     → "📋 Epics" page
├── diagrams/
│   └── (Auto-generated from mermaid)
└── NOTION_IMPORT.md          → This file

Recommended Notion Structure:
📊 BSide Project (Database)
  ├── 📋 Overview
  ├── 🎯 Kanban Board
  ├── 🏃 Scrum Sprint
  ├── 📈 Roadmap
  ├── 🐛 Bugs (Database)
  ├── 📚 Documentation
  └── 📊 Metrics
```

---

## 🎨 Notion Database Setup

### 1. Create "Stories" Database

**Properties:**
| Property | Type | Options |
|----------|------|---------|
| ID | Title | - |
| Summary | Text | - |
| Status | Select | Backlog, To Do, In Progress, Review, Testing, Done |
| Priority | Select | P0 (Critical), P1 (High), P2 (Medium), P3 (Low) |
| Story Points | Number | - |
| Assignee | Person | - |
| Sprint | Relation | → Sprints database |
| Epic | Relation | → Epics database |
| Tags | Multi-select | Frontend, Backend, DevOps, Docs, etc. |
| Created | Created time | - |
| Updated | Last edited time | - |

### 2. Create "Sprints" Database

**Properties:**
| Property | Type | Options |
|----------|------|---------|
| Sprint Name | Title | - |
| Start Date | Date | - |
| End Date | Date | - |
| Status | Select | Planning, Active, Review, Complete |
| Goal | Text | - |
| Velocity | Number | Story points |
| Stories | Relation | → Stories database |

### 3. Create "Epics" Database

**Properties:**
| Property | Type | Options |
|----------|------|---------|
| Epic Name | Title | - |
| Description | Text | - |
| Status | Select | Not Started, In Progress, Complete |
| Owner | Person | - |
| Target Date | Date | - |
| Stories | Relation | → Stories database |

---

## 📊 Import Current Data

### Stories to Import

```csv
ID,Summary,Status,Priority,Points,Assignee,Tags
BSI-201,Message Reactions UI,Done,P1,5,@david,Frontend
BSI-202,Reactions Backend,To Do,P1,5,@alice,Backend
BSI-203,CI/CD Optimization,Done,P1,8,@bob,DevOps
BSI-204,CI/CD Test Filtering,In Progress,P1,3,@bob,DevOps
BSI-205,Local Dev Documentation,Done,P2,5,@carol,Docs
BSI-206,App Store Metadata,To Do,P1,8,@marketing,Marketing
BSI-207,Google Play Metadata,To Do,P2,8,@marketing,Marketing
BSI-208,Privacy Policy & ToS,To Do,P2,5,@legal,Legal
BSI-209,Store Listing Assets,To Do,P2,5,@design,Design
BSI-210,TestFlight Beta Setup,To Do,P2,3,@bob,DevOps
```

**Import Steps:**
1. Copy CSV above
2. Create new database in Notion
3. Click "..." → Import → CSV
4. Paste data
5. Map columns to properties

### Sprints to Import

```csv
Sprint Name,Start Date,End Date,Status,Goal,Velocity
Sprint 8,2026-01-21,2026-01-27,Active,Complete reactions & optimize CI/CD,26
Sprint 9,2026-01-28,2026-02-03,Planning,Store deployment prep,29
Sprint 10,2026-02-04,2026-02-10,Planning,Beta testing & polish,27
```

---

## 🎯 Kanban Board Views

### View 1: By Status
- Group by: Status
- Sort by: Priority (ascending)
- Filter: None
- Show: All properties

### View 2: By Assignee
- Group by: Assignee
- Sort by: Priority (ascending)
- Filter: Status ≠ Done
- Show: ID, Summary, Priority, Points

### View 3: Current Sprint
- Group by: Status
- Sort by: Priority (ascending)
- Filter: Sprint = "Sprint 8"
- Show: All properties

---

## 📈 Dashboard Widgets

### 1. Sprint Progress
```
Formula: prop("Completed Points") / prop("Committed Points") * 100
Display: Progress bar
```

### 2. Velocity Chart
```
Database: Sprints
Group by: Sprint Name
Sum: Velocity
Chart type: Bar
```

### 3. Team Capacity
```
Database: Stories
Group by: Assignee
Sum: Story Points
Filter: Status = "In Progress" or "To Do"
```

### 4. Bug Count
```
Database: Stories
Filter: Tags contains "Bug"
Count: Stories
Color: Red if > 5
```

---

## 🔗 Linked Databases

### In "Project Overview" Page:
1. **Current Sprint** (Stories filtered by current sprint)
2. **Active Epics** (Epics filtered by Status = In Progress)
3. **My Tasks** (Stories filtered by Assignee = @me)
4. **Blocked Items** (Stories filtered by Status = Blocked)

### In "Roadmap" Page:
1. **Timeline View** (All epics on timeline)
2. **Board View** (Epics grouped by status)
3. **Calendar View** (Stories by due date)

---

## 🎨 Templates

### Story Template
```markdown
## Description
[Brief description of the story]

## Acceptance Criteria
- [ ] Criteria 1
- [ ] Criteria 2
- [ ] Criteria 3

## Technical Notes
[Implementation details]

## Dependencies
- Depends on: [BSI-XXX]
- Blocked by: [None]

## Testing Notes
[How to test this feature]

## Demo
[Link to demo video or screenshots]
```

### Sprint Template
```markdown
## Sprint Goal
[What we aim to achieve this sprint]

## Team Capacity
- Total hours: 120
- Available developers: 6
- Velocity target: 27 points

## Committed Stories
[Stories pulled into sprint]

## Daily Notes
### Day 1
- Progress:
- Blockers:

### Day 2
...

## Sprint Review
### Completed
- [Stories completed]

### Carried Over
- [Stories not completed]

### Retrospective Actions
**Start:**
**Stop:**
**Continue:**
```

---

## 🔔 Notion Automations

### 1. Status Change Notifications
```
When: Story status changes to "Done"
Then: Send notification to #team-wins Slack channel
```

### 2. Blocked Item Alerts
```
When: Story status changes to "Blocked"
Then: Send email to Product Owner + Scrum Master
```

### 3. Sprint End Reminder
```
When: 2 days before sprint end date
Then: Send reminder to team in Slack
```

### 4. Overdue Task Escalation
```
When: Story due date passes AND status ≠ Done
Then: Send daily reminder to assignee + manager
```

---

## 📊 Reports to Create

### 1. Sprint Burndown
- Chart: Line
- X-axis: Day
- Y-axis: Story points remaining
- Data: Daily snapshot of incomplete points

### 2. Velocity Trend
- Chart: Bar
- X-axis: Sprint
- Y-axis: Completed points
- Data: Sum of completed story points per sprint

### 3. Cycle Time Distribution
- Chart: Histogram
- X-axis: Days (0-1, 1-2, 2-3, 3+)
- Y-axis: Count of stories
- Data: Time from "In Progress" to "Done"

### 4. Team Throughput
- Chart: Stacked bar
- X-axis: Week
- Y-axis: Story count
- Group by: Team member
- Data: Count of stories completed per person per week

---

## 🎯 Custom Filters

### High Priority Open Items
```
Filter:
- Status: To Do OR In Progress
- Priority: P0 OR P1
Sort: Priority ascending, Created descending
```

### My Active Work
```
Filter:
- Assignee: @me
- Status: In Progress
Sort: Priority ascending
```

### Technical Debt
```
Filter:
- Tags: contains "Tech Debt"
- Status: ≠ Done
Sort: Created ascending (oldest first)
```

### Bugs by Severity
```
Filter:
- Tags: contains "Bug"
- Status: ≠ Done
Group by: Priority
Sort: Created descending
```

---

## 📱 Mobile App Setup

### Quick Actions
1. Add to "My Tasks" view widget
2. Enable notifications for:
   - Stories assigned to me
   - Comments on my stories
   - Sprint milestones
3. Offline mode: Enable for key databases

### Favorites
- Current Sprint board
- My Tasks view
- Team Calendar
- Documentation

---

## 🔐 Permissions Setup

### Role-Based Access

**Admin** (Product Owner, Scrum Master):
- Full access to all pages
- Can edit databases
- Can create/delete pages
- Can manage permissions

**Developer** (Team Members):
- Edit access to Stories database
- Comment on all pages
- View all documentation
- Cannot delete pages

**Stakeholder** (Management, Clients):
- View-only access to:
  - Project Overview
  - Roadmap
  - Sprint boards
- Cannot view:
  - Team capacity details
  - Salary information
  - Internal processes

**Guest** (External):
- View-only access to specific pages
- Time-limited access (30 days)
- Watermarked documents

---

## 📚 Integration Setup

### GitHub
1. Install Notion GitHub integration
2. Connect repository: `brentmzey/lovebside`
3. Link PRs to stories (use `[BSI-XXX]` in PR title)
4. Auto-update story status on PR merge

### Slack
1. Install Notion Slack app
2. Enable notifications to #bside-dev
3. Create `/notion` slash command for quick access
4. Daily standup bot posts to Slack

### Calendar
1. Sync Sprint dates to team calendar
2. Add Sprint Review/Retro meetings automatically
3. Sync personal task due dates

---

## 🎨 Styling & Branding

### Cover Images
- Project page: Use BSide logo banner
- Sprint pages: Use sprint-themed images
- Documentation: Use relevant tech stack logos

### Icons
- 📊 Project Management
- 🎯 Kanban Board
- 🏃 Scrum Board
- 📋 Epics
- 🐛 Bugs
- 📚 Documentation
- 📈 Metrics
- 🚀 Releases

### Colors
- P0 (Critical): 🔴 Red
- P1 (High): 🟠 Orange
- P2 (Medium): 🟡 Yellow
- P3 (Low): 🟢 Green
- Done: 🔵 Blue
- Blocked: ⚫ Black

---

## ✅ Post-Import Checklist

After importing to Notion:
- [ ] Verify all pages imported correctly
- [ ] Set up database relations
- [ ] Configure views (Kanban, Calendar, etc.)
- [ ] Apply templates
- [ ] Set permissions
- [ ] Configure integrations (GitHub, Slack)
- [ ] Create dashboards
- [ ] Set up automations
- [ ] Train team on Notion usage
- [ ] Schedule first sync meeting

---

## 🔄 Sync Strategy

### Keep .code_hq Updated
1. Export from Notion weekly (Markdown format)
2. Update `.code_hq` files in repository
3. Commit changes to Git
4. This keeps code and project management in sync

### Automated Sync (Future)
- GitHub Action to auto-update Notion from `.code_hq`
- Notion API integration for two-way sync
- Conflict resolution strategy

---

## 🎓 Team Training

### Onboarding Resources
1. **Notion Basics** (30 min video)
2. **Our Workspace Tour** (Live demo)
3. **Database Training** (Hands-on practice)
4. **Mobile App Setup** (Self-paced)

### Best Practices
- Update story status daily
- Add comments for context
- Use @mentions for collaboration
- Link related pages
- Keep descriptions concise
- Use templates consistently

---

## 📞 Support

**Questions?** Ask in #notion-help Slack channel  
**Issues?** Report to @product  
**Feature Requests?** Add to "Notion Improvements" page

---

**Import Guide Version:** 1.0  
**Last Updated:** 2026-01-24  
**Maintained By:** @carol (Documentation Team)
