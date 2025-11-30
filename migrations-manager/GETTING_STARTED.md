# Getting Started with PocketBase Migrations Manager

## What is this?

This is a TypeScript-based tool for managing PocketBase database migrations programmatically. It provides:
- ✅ Secure credential management (env vars, AWS Secrets, or custom config servers)
- ✅ Migration tracking and versioning
- ✅ Rollback capabilities
- ✅ CLI tools for easy migration management
- ✅ CI/CD integration with GitHub Actions

## Installation

```bash
cd migrations-manager
npm install
```

## Quick Start (Local Development)

1. **Start your local PocketBase** (via Docker Compose):
   ```bash
   cd ..
   docker-compose up pocketbase -d
   ```

2. **Copy local config**:
   ```bash
   cd migrations-manager
   cp .env.local.example .env
   ```

3. **Check connection**:
   ```bash
   npm run migrate:status
   ```

## Create Your First Migration

```bash
npm run migrate:create "add posts collection"
```

This creates a file like `20231117101500_add_posts_collection.js` in `../pocketbase/migrations/`.

Edit the migration file:
```javascript
migrate((db) => {
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
        name: "body",
        type: "editor",
        required: false,
      },
      {
        name: "author",
        type: "relation",
        required: true,
        options: {
          collectionId: "users",
          cascadeDelete: false,
        },
      },
    ],
  });
  
  return db.saveCollection(collection);
});
```

## Apply Migrations

```bash
# Run all pending migrations
npm run migrate

# Or run one at a time
npm run migrate:up
```

## Rollback Migrations

```bash
npm run migrate:down
```

## Production Setup

### Using AWS Secrets Manager

1. **Create AWS Secret**:
   ```bash
   aws secretsmanager create-secret \
     --name bside/pocketbase/production \
     --secret-string '{
       "POCKETBASE_URL": "https://your-instance.pockethost.io",
       "POCKETBASE_ADMIN_EMAIL": "admin@example.com",
       "POCKETBASE_ADMIN_PASSWORD": "super-secure-password"
     }'
   ```

2. **Update `.env`**:
   ```env
   SECRETS_PROVIDER=aws
   AWS_REGION=us-east-1
   AWS_SECRET_NAME=bside/pocketbase/production
   MIGRATIONS_DIR=../pocketbase/migrations
   ```

3. **Configure AWS credentials** (one of):
   - IAM role (recommended for EC2/ECS/Lambda)
   - AWS CLI: `aws configure`
   - Environment variables: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`

4. **Run migrations**:
   ```bash
   npm run migrate
   ```

## CI/CD Integration

The GitHub Actions workflow (`.github/workflows/migrations.yml`) automatically:
- ✅ Validates migrations on pull requests
- ✅ Applies to staging when merging to `develop`
- ✅ Applies to production when merging to `main`

**Setup Required GitHub Secrets:**
1. Go to your repository → Settings → Secrets and variables → Actions
2. Add these secrets:
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`
   - `AWS_REGION` (e.g., `us-east-1`)
   - `AWS_SECRET_NAME_STAGING` (e.g., `bside/pocketbase/staging`)
   - `AWS_SECRET_NAME_PRODUCTION` (e.g., `bside/pocketbase/production`)

## Common Commands

Using npm:
```bash
npm run migrate:status    # Check which migrations are pending
npm run migrate           # Run all pending migrations
npm run migrate:up        # Run next migration
npm run migrate:down      # Rollback last batch
npm run migrate:create "name"  # Create new migration
```

Using the shell script:
```bash
./migrate.sh status
./migrate.sh              # Run all
./migrate.sh up
./migrate.sh down
./migrate.sh create "add feature"
```

Using Make (if you have Make installed):
```bash
make status
make migrate
make up
make down
make create NAME="add feature"
```

## File Structure

```
migrations-manager/
├── src/
│   ├── cli.ts                    # CLI commands
│   ├── index.ts                  # Library exports
│   ├── config/
│   │   ├── index.ts              # Config loader
│   │   ├── aws-secrets.ts        # AWS Secrets Manager integration
│   │   └── microconfig.ts        # Custom config server integration
│   ├── services/
│   │   ├── pocketbase.ts         # PocketBase client wrapper
│   │   └── migration-manager.ts  # Migration logic
│   ├── types/
│   │   └── index.ts              # TypeScript types
│   └── utils/
│       └── format.ts             # Formatting utilities
├── .env.example                  # Production config template
├── .env.local.example            # Local development config template
├── package.json
├── tsconfig.json
├── README.md                     # Full documentation
├── SETUP.md                      # Detailed setup guide
└── GETTING_STARTED.md            # This file
```

## Troubleshooting

### "Authentication failed"
- Check your PocketBase URL (include `https://` or `http://`)
- Verify admin email and password
- Ensure admin account exists

### "Cannot connect to PocketBase"
- For local: ensure PocketBase is running (`docker-compose ps`)
- For remote: check URL is accessible
- Verify firewall/network settings

### AWS credential errors
- Ensure AWS CLI is configured: `aws sts get-caller-identity`
- Check IAM permissions include `secretsmanager:GetSecretValue`
- Verify secret name and region

## Next Steps

1. Read [SETUP.md](./SETUP.md) for detailed configuration options
2. Read [README.md](./README.md) for full API documentation
3. Check existing migrations in `../pocketbase/migrations/`
4. Create a test migration and apply it locally
5. Set up AWS Secrets Manager for staging/production

## Need Help?

- Check the documentation files in this directory
- Review existing migration files for examples
- Check PocketBase documentation: https://pocketbase.io/docs/
