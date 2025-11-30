# Migrations Manager Setup Guide

## Quick Start

1. **Install dependencies:**
   ```bash
   cd migrations-manager
   npm install
   ```

2. **Configure credentials:**
   
   Copy the example environment file:
   ```bash
   cp .env.example .env
   ```

   Edit `.env` with your PocketHost credentials:
   ```env
   POCKETBASE_URL=https://your-instance.pockethost.io
   POCKETBASE_ADMIN_EMAIL=admin@example.com
   POCKETBASE_ADMIN_PASSWORD=your-secure-password
   MIGRATIONS_DIR=../pocketbase/migrations
   ```

3. **Test connection:**
   ```bash
   npm run migrate:status
   ```

## Credential Management Options

### Option 1: Environment Variables (Development)

Best for local development. Create `.env` file:

```env
POCKETBASE_URL=https://your-instance.pockethost.io
POCKETBASE_ADMIN_EMAIL=admin@example.com
POCKETBASE_ADMIN_PASSWORD=your-secure-password
MIGRATIONS_DIR=../pocketbase/migrations
```

### Option 2: AWS Secrets Manager (Recommended for Production)

1. **Create a secret in AWS Secrets Manager:**
   ```bash
   aws secretsmanager create-secret \
     --name bside/pocketbase/production \
     --description "PocketBase credentials for production" \
     --secret-string '{
       "POCKETBASE_URL": "https://your-instance.pockethost.io",
       "POCKETBASE_ADMIN_EMAIL": "admin@example.com",
       "POCKETBASE_ADMIN_PASSWORD": "your-secure-password"
     }'
   ```

2. **Configure `.env` to use AWS:**
   ```env
   SECRETS_PROVIDER=aws
   AWS_REGION=us-east-1
   AWS_SECRET_NAME=bside/pocketbase/production
   MIGRATIONS_DIR=../pocketbase/migrations
   ```

3. **Ensure AWS credentials are configured:**
   - Use IAM role (recommended for EC2/ECS)
   - Or configure AWS CLI: `aws configure`
   - Or set environment variables: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`

### Option 3: Custom Microconfig Server

If you have a custom configuration server:

```env
SECRETS_PROVIDER=microconfig
MICROCONFIG_URL=https://config.example.com/api
MICROCONFIG_TOKEN=your-api-token
MIGRATIONS_DIR=../pocketbase/migrations
```

Your microconfig server should respond to `GET /secrets/pocketbase` with:
```json
{
  "POCKETBASE_URL": "https://your-instance.pockethost.io",
  "POCKETBASE_ADMIN_EMAIL": "admin@example.com",
  "POCKETBASE_ADMIN_PASSWORD": "your-secure-password"
}
```

## Usage

### Check migration status
```bash
npm run migrate:status
```

### Run all pending migrations
```bash
npm run migrate
```

### Run single migration
```bash
npm run migrate:up
```

### Rollback last batch
```bash
npm run migrate:down
```

### Create new migration
```bash
npm run migrate:create "add user profiles"
```

### Using the shell script
```bash
./migrate.sh status
./migrate.sh                    # Run all pending
./migrate.sh up                 # Run one
./migrate.sh down               # Rollback
./migrate.sh create "new thing" # Create migration
```

## CI/CD Setup

### GitHub Actions

The project includes a GitHub Actions workflow (`.github/workflows/migrations.yml`) that:

1. Validates migrations on PR
2. Auto-applies migrations to staging on push to `develop`
3. Auto-applies migrations to production on push to `main`
4. Allows manual workflow dispatch to any environment

**Required GitHub Secrets:**
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION`
- `AWS_SECRET_NAME_STAGING`
- `AWS_SECRET_NAME_PRODUCTION`

### Manual Deployment

For manual deployment to production:

```bash
# Set up production credentials
export SECRETS_PROVIDER=aws
export AWS_SECRET_NAME=bside/pocketbase/production
export AWS_REGION=us-east-1

# Check what will be applied
npm run migrate:status

# Apply migrations
npm run migrate
```

## Migration File Structure

Generated migration files follow this structure:

```javascript
/// <reference path="../pb_data/types.d.ts" />

migrate((db) => {
  // UP: Apply changes
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
  // DOWN: Rollback changes (optional)
  const collection = db.findCollectionByNameOrId("posts");
  return db.deleteCollection(collection);
});
```

## Troubleshooting

### Authentication Failed
- Verify PocketBase URL is correct (include https://)
- Check admin email and password
- Ensure admin account exists in PocketBase

### AWS Credentials Error
- Verify AWS credentials are configured
- Check IAM permissions include `secretsmanager:GetSecretValue`
- Verify secret name and region are correct

### Migration Failed
- Check migration file syntax
- Review PocketBase logs for errors
- Ensure collection/field names are unique
- Test migration in development first

## Best Practices

1. **Always test locally first**
2. **Use descriptive migration names**
3. **Keep migrations atomic** - one logical change per migration
4. **Never modify applied migrations** - create new ones instead
5. **Include rollback logic** when possible
6. **Review migrations in PRs** before merging
7. **Monitor migration runs** in production
8. **Backup data** before major schema changes

## Security Checklist

- [ ] `.env` file is in `.gitignore`
- [ ] Production credentials stored in AWS Secrets Manager
- [ ] AWS IAM follows least-privilege principle
- [ ] MFA enabled on admin accounts
- [ ] Credentials rotated regularly
- [ ] GitHub repository secrets configured
- [ ] No credentials in migration files
