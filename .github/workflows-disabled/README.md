# GitHub Workflows - Disabled

This directory contains GitHub Actions workflows that are **DISABLED** and not active.

These are kept as examples for future use when you're ready to enable CI/CD automation.

## Available Workflows

### migrations.yml.example
PocketBase migrations workflow that can:
- Validate migrations on pull requests
- Auto-deploy to staging/production
- Manual workflow dispatch

**To enable:** Rename to `.github/workflows/migrations.yml` and configure secrets.

## Cost Considerations

GitHub Actions consume billable minutes. Only enable workflows when:
1. You've set up the required secrets
2. You want automatic deployment
3. You understand the billing implications

## Manual Alternative

Instead of CI/CD, you can run migrations manually:

```bash
cd migrations-manager
npm run migrate:status
npm run migrate
```
