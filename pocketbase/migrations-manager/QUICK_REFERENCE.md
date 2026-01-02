# PocketBase Migrations Manager - Quick Reference

## 🚀 Quick Setup (Local Dev)

```bash
cd migrations-manager
npm install
cp .env.local.example .env
npm run migrate:status
```

## 📝 Common Commands

| Task | Command |
|------|---------|
| Check status | `npm run migrate:status` |
| Run all pending | `npm run migrate` |
| Run one migration | `npm run migrate:up` |
| Rollback last batch | `npm run migrate:down` |
| Create new migration | `npm run migrate:create "migration name"` |

## 🔐 Environment Variables

### Local Development
```env
POCKETBASE_URL=http://localhost:8090
POCKETBASE_ADMIN_EMAIL=admin@bside.love
POCKETBASE_ADMIN_PASSWORD=changeme
MIGRATIONS_DIR=../pocketbase/migrations
```

### AWS Secrets Manager
```env
SECRETS_PROVIDER=aws
AWS_REGION=us-east-1
AWS_SECRET_NAME=bside/pocketbase/production
MIGRATIONS_DIR=../pocketbase/migrations
```

### Custom Config Server
```env
SECRETS_PROVIDER=microconfig
MICROCONFIG_URL=https://config.example.com/api
MICROCONFIG_TOKEN=your-token
MIGRATIONS_DIR=../pocketbase/migrations
```

## 📦 Migration File Template

```javascript
/// <reference path="../pb_data/types.d.ts" />
migrate((db) => {
  // UP: Apply changes
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
  // DOWN: Rollback changes
  const collection = db.findCollectionByNameOrId("example");
  return db.deleteCollection(collection);
});
```

## 🛠️ Programmatic Usage

```typescript
import { loadConfig, PocketBaseClient, MigrationManager } from '@bside/migrations-manager';

async function migrate() {
  const config = await loadConfig();
  const client = new PocketBaseClient(config);
  await client.authenticate();
  await client.ensureMigrationsCollection();
  
  const manager = new MigrationManager(config, client);
  const results = await manager.runPendingMigrations();
  console.log(results);
}
```

## 🔄 CI/CD Setup

### GitHub Actions Workflow
Location: `.github/workflows/migrations.yml`

### Required Secrets
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION`
- `AWS_SECRET_NAME_STAGING`
- `AWS_SECRET_NAME_PRODUCTION`

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| Authentication failed | Check URL, email, password in config |
| Cannot connect | Ensure PocketBase is running and accessible |
| AWS credentials error | Run `aws sts get-caller-identity` to verify |
| Migration failed | Check PocketBase logs and migration syntax |

## 📚 Documentation Files

- **GETTING_STARTED.md** - Quick start tutorial
- **SETUP.md** - Detailed configuration guide
- **README.md** - Full API documentation
- **QUICK_REFERENCE.md** - This file

## 💡 Best Practices

1. ✅ Always test migrations locally first
2. ✅ Use descriptive migration names
3. ✅ One logical change per migration
4. ✅ Never modify applied migrations
5. ✅ Include rollback logic when possible
6. ✅ Review migrations in PRs
7. ✅ Backup data before major changes
8. ✅ Monitor migration runs in production
