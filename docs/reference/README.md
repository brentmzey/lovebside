# B-Side Documentation for ReadMe

This directory contains documentation structured for publishing to [ReadMe.com](https://readme.com).

## Directory Structure

```
readme-docs/
├── getting-started/          # Getting Started guides
│   ├── introduction.md
│   ├── quick-start.md
│   ├── installation.md
│   └── first-app.md
├── guides/                   # User guides
│   ├── building.md
│   ├── running.md
│   ├── testing.md
│   └── deployment.md
├── api-reference/           # API documentation
│   ├── overview.md
│   ├── authentication.md
│   ├── endpoints/
│   └── websockets.md
├── architecture/            # Architecture docs
│   ├── overview.md
│   ├── multiplatform.md
│   ├── messaging.md
│   └── database.md
├── platform-guides/         # Platform-specific guides
│   ├── android.md
│   ├── ios.md
│   ├── desktop.md
│   └── web.md
├── reference/               # Reference materials
│   ├── cli-commands.md
│   ├── configuration.md
│   └── troubleshooting.md
└── changelog/               # Version history
    └── releases.md
```

## Publishing to ReadMe

### Prerequisites
1. Sign up at https://dash.readme.com/
2. Create a new project
3. Get your API key from Settings > API Keys

### Publishing Methods

#### Method 1: Using ReadMe's GitHub Integration
1. Connect your GitHub repository in ReadMe dashboard
2. Point to this `readme-docs` directory
3. Enable auto-sync

#### Method 2: Using ReadMe CLI
```bash
npm install -g rdme
rdme docs readme-docs --key=YOUR_API_KEY
```

#### Method 3: Using ReadMe API
```bash
# Upload a single document
curl https://dash.readme.com/api/v1/docs \
  -u YOUR_API_KEY: \
  -H "Content-Type: application/json" \
  -d @document.json
```

## ReadMe Markdown Extensions

ReadMe supports enhanced Markdown with special blocks:

### Code Blocks with Language Tabs
```
[block:code]
{
  "codes": [
    {
      "code": "npm install bside",
      "language": "shell",
      "name": "npm"
    },
    {
      "code": "yarn add bside",
      "language": "shell",
      "name": "yarn"
    }
  ]
}
[/block]
```

### Callouts
```markdown
> 📘 Info
> This is an info callout

> ❗️ Warning
> This is a warning callout

> 🚧 Under Construction
> This feature is coming soon
```

### API Definitions
Use OpenAPI/Swagger specs for automatic API reference generation.

## Document Metadata

Each markdown file should include frontmatter:

```yaml
---
title: "Quick Start Guide"
excerpt: "Get up and running with B-Side in 5 minutes"
category: "getting-started"
slug: "quick-start"
order: 1
---
```

## Next Steps

1. Review and customize the documentation structure
2. Migrate content from `/docs` to this structure
3. Add ReadMe-specific formatting and features
4. Set up API reference with OpenAPI specs
5. Configure webhooks for auto-deployment
6. Add interactive examples and code runners

## Resources

- [ReadMe Documentation](https://docs.readme.com/)
- [ReadMe Markdown Guide](https://docs.readme.com/docs/rdme)
- [ReadMe API Reference](https://docs.readme.com/reference)
