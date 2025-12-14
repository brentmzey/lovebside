# PocketBase Migrations Manager

TypeScript-based migration management tooling for PocketBase with support for secure credential management via AWS Secrets Manager, environment variables, or custom microconfig servers.

## Features

- 🔐 **Secure Credential Management**: Support for AWS Secrets Manager, environment variables, or custom config servers
- 📦 **Migration Tracking**: Automatic tracking of applied migrations
- 🔄 **Rollback Support**: Easy rollback of migration batches
- 📝 **Migration Generation**: CLI tools to generate new migration files
- 🎯 **Type-Safe**: Built with TypeScript for type safety
- 🚀 **PocketHost Compatible**: Designed to work with PocketHost instances

## Installation

```bash
cd migrations-manager
npm install
```

## Configuration

Create a `.env` file based on `.env.example`:

### Using Environment Variables (Default)

```env
POCKETBASE_URL=https://your-instance.pockethost.io
POCKETBASE_ADMIN_EMAIL=admin@example.com
POCKETBASE_ADMIN_PASSWORD=your-secure-password
MIGRATIONS_DIR=../pocketbase/migrations
MIGRATION_TABLE=pb_migrations
```

> ℹ️ **Note:** Keep the migration tracking collection name free of leading underscores. PocketBase reserves names like `_migrations`, so use the default `pb_migrations` (or another custom name) unless you have a strong reason to change it.

### Using AWS Secrets Manager

```env
SECRETS_PROVIDER=aws
AWS_REGION=us-east-1
AWS_SECRET_NAME=bside/pocketbase/credentials
MIGRATIONS_DIR=../pocketbase/migrations
```

Your AWS Secret should contain:
```json
{
  "POCKETBASE_URL": "https://your-instance.pockethost.io",
  "POCKETBASE_ADMIN_EMAIL": "admin@example.com",
  "POCKETBASE_ADMIN_PASSWORD": "your-secure-password"
}
```

### Using Microconfig Server

```env
SECRETS_PROVIDER=microconfig
MICROCONFIG_URL=https://config.example.com/api
MICROCONFIG_TOKEN=your-token
MIGRATIONS_DIR=../pocketbase/migrations
```

## Usage

### Check Migration Status

```bash
npm run migrate:status
```

### Run All Pending Migrations

```bash
npm run migrate
```

### Run Next Migration

```bash
npm run migrate:up
```

### Rollback Last Batch

```bash
npm run migrate:down
```

### Create New Migration

```bash
npm run migrate:create "add users table"
```

This creates a new migration file in the migrations directory with a timestamp.

## Migration File Structure

Migration files are JavaScript files with the following structure:

```javascript
/// <reference path="../pb_data/types.d.ts" />
migrate((db) => {
  // Migration up code
  const collection = new Collection({
    name: "example",
    type: "base",
    schema: [
      {
        name: "field_name",
        type: "text",
        required: true,
      },
    ],
  });
  
  return db.saveCollection(collection);
}, (db) => {
  // Migration down code (rollback)
  const collection = db.findCollectionByNameOrId("example");
  return db.deleteCollection(collection);
});
```

## Programmatic Usage

```typescript
import { loadConfig, PocketBaseClient, MigrationManager } from '@bside/migrations-manager';

async function runMigrations() {
  const config = await loadConfig();
  const client = new PocketBaseClient(config);
  
  await client.authenticate();
  await client.ensureMigrationsCollection();
  
  const manager = new MigrationManager(config, client);
  const results = await manager.runPendingMigrations();
  
  console.log('Migrations completed:', results);
}
```

## Security Best Practices

1. **Never commit `.env` files** - Always use `.env.example` as a template
2. **Use AWS Secrets Manager or similar** for production environments
3. **Rotate credentials regularly**
4. **Use least-privilege IAM roles** when using AWS
5. **Enable MFA** on admin accounts

## Development

```bash
# Build the project
npm run build

# Run in development mode
npm run dev

# Lint code
npm run lint

# Format code
npm run format

# Run tests
npm test
```

## License

MIT
