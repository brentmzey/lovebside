# PocketBase Migrations Manager

## Overview

The `migrations-manager/` directory contains a complete TypeScript/JavaScript tooling solution for managing PocketBase database migrations programmatically. This tool integrates with your PocketHost-hosted PocketBase instance and provides secure, version-controlled database schema management.

## Key Features

- **Secure Credential Management**: Multiple options for storing credentials
  - Environment variables (local development)
  - AWS Secrets Manager (production - recommended)
  - Custom microconfig server (flexible)
  
- **Migration Management**: Full lifecycle management
  - Create migrations with templates
  - Apply migrations with tracking
  - Rollback capabilities
  - Migration status checking
  
- **Developer Experience**: Multiple interfaces
  - npm scripts
  - Shell script (`./migrate.sh`)
  - Makefile commands
  - Programmatic API
  
- **CI/CD Ready**: GitHub Actions workflow included
  - Automatic validation on PRs
  - Auto-deployment to staging/production
  - Manual workflow dispatch

## Quick Links

- 📖 [Getting Started Guide](../migrations-manager/GETTING_STARTED.md)
- 🔧 [Setup Instructions](../migrations-manager/SETUP.md)
- 📋 [Quick Reference](../migrations-manager/QUICK_REFERENCE.md)
- 📚 [Full Documentation](../migrations-manager/README.md)

## Quick Start

```bash
# Navigate to migrations manager
cd migrations-manager

# Install dependencies
npm install

# Configure for local development
cp .env.local.example .env

# Check connection
npm run migrate:status

# Create a new migration
npm run migrate:create "add posts collection"

# Apply migrations
npm run migrate
```

### One-liner via helper script

From the repo root you can now run:

```bash
./scripts/run-pocketbase-migrations.sh migrate          # apply pending migrations
./scripts/run-pocketbase-migrations.sh migrate:status   # show status
./scripts/run-pocketbase-migrations.sh migrate:create "add posts"
```

The script installs dependencies on first run and forwards any additional npm arguments, so it works in CI or local shells without extra setup.

## Project Structure

```
migrations-manager/
├── src/
│   ├── cli.ts                    # CLI interface
│   ├── index.ts                  # Library exports
│   ├── config/                   # Configuration management
│   │   ├── index.ts              # Config loader
│   │   ├── aws-secrets.ts        # AWS integration
│   │   └── microconfig.ts        # Custom config server
│   ├── services/                 # Core services
│   │   ├── pocketbase.ts         # PocketBase client
│   │   └── migration-manager.ts  # Migration logic
│   ├── types/                    # TypeScript types
│   └── utils/                    # Utilities
├── .env.example                  # Production config template
├── .env.local.example            # Local dev config template
├── package.json                  # Dependencies
├── tsconfig.json                 # TypeScript config
├── README.md                     # Documentation
├── SETUP.md                      # Setup guide
├── GETTING_STARTED.md            # Quick start
└── QUICK_REFERENCE.md            # Command reference
```

## Credential Management

### Local Development (Environment Variables)

```env
POCKETBASE_URL=http://localhost:8090
POCKETBASE_ADMIN_EMAIL=admin@bside.love
POCKETBASE_ADMIN_PASSWORD=changeme
MIGRATIONS_DIR=../pocketbase/migrations
MIGRATION_TABLE=pb_migrations
```

> ⚠️ **Important:** PocketBase reserves collection names that start with `_` (for example `_migrations`). Use `pb_migrations` (the default above) or another non-reserved name for `MIGRATION_TABLE` so the CLI can create/read the tracking collection.

### Production (AWS Secrets Manager)

```env
SECRETS_PROVIDER=aws
AWS_REGION=us-east-1
AWS_SECRET_NAME=bside/pocketbase/production
MIGRATIONS_DIR=../pocketbase/migrations
```

Create the AWS secret:
```bash
aws secretsmanager create-secret \
  --name bside/pocketbase/production \
  --secret-string '{
    "POCKETBASE_URL": "https://your-instance.pockethost.io",
    "POCKETBASE_ADMIN_EMAIL": "admin@example.com",
    "POCKETBASE_ADMIN_PASSWORD": "secure-password"
  }'
```

## Common Commands

| Task | Command |
|------|---------|
| Check status | `npm run migrate:status` |
| Run all pending | `npm run migrate` |
| Run one migration | `npm run migrate:up` |
| Rollback last batch | `npm run migrate:down` |
| Create migration | `npm run migrate:create "name"` |

## CI/CD Integration

GitHub Actions workflow at `.github/workflows/migrations.yml` automatically:
- ✅ Validates migrations on pull requests
- ✅ Deploys to staging on push to `develop` branch
- ✅ Deploys to production on push to `main` branch
- ✅ Supports manual workflow dispatch

### Required GitHub Secrets

Set these in your repository settings:
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION`
- `AWS_SECRET_NAME_STAGING`
- `AWS_SECRET_NAME_PRODUCTION`

## Migration File Example

```javascript
/// <reference path="../pb_data/types.d.ts" />

migrate((db) => {
  // Apply changes
  const collection = new Collection({
    name: "posts",
    type: "base",
    schema: [
      {
        name: "title",
        type: "text",
        required: true,
      },
      {
        name: "content",
        type: "editor",
        required: false,
      },
    ],
  });
  
  return db.saveCollection(collection);
}, (db) => {
  // Rollback changes
  const collection = db.findCollectionByNameOrId("posts");
  return db.deleteCollection(collection);
});
```

## Best Practices

1. **Always test locally** before deploying to production
2. **Use descriptive names** for migrations
3. **Keep migrations atomic** - one logical change per migration
4. **Never modify applied migrations** - create new ones instead
5. **Include rollback logic** when possible
6. **Review migrations in PRs** before merging
7. **Monitor migration runs** in production
8. **Backup data** before major schema changes

## Troubleshooting

### Authentication Failed
- Verify PocketBase URL includes protocol (http:// or https://)
- Check admin email and password are correct
- Ensure admin account exists in PocketBase

### Cannot Connect
- For local: ensure PocketBase is running (`docker-compose ps`)
- For remote: verify URL is accessible
- Check firewall/network settings

### AWS Credentials Error
- Verify AWS CLI is configured: `aws sts get-caller-identity`
- Check IAM permissions include `secretsmanager:GetSecretValue`
- Confirm secret name and region are correct

## Support

For detailed information, see:
- [GETTING_STARTED.md](../migrations-manager/GETTING_STARTED.md) - Step-by-step tutorial
- [SETUP.md](../migrations-manager/SETUP.md) - Comprehensive setup guide
- [README.md](../migrations-manager/README.md) - Full API documentation
- [QUICK_REFERENCE.md](../migrations-manager/QUICK_REFERENCE.md) - Command cheat sheet

## Related Documentation

- [PocketBase Documentation](https://pocketbase.io/docs/)
- [PocketHost Documentation](https://pockethost.io/docs/)
- [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/)
