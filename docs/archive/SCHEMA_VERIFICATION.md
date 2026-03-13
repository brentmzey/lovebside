# Schema Consistency Verification

## Goal

Ensure **identical schema** across all environments:

- ✅ Production (PocketHost)
- ✅ Local development
- ✅ Docker Compose
- ✅ CI/CD pipelines
- ✅ Any staging/VM environment

## Strategy

### 1. Migration-Based Approach (Current)

- Migrations are version-controlled
- Applied in same order everywhere
- Checksum validation prevents tampering

### 2. Schema Validation Tool (New)

- Export schema from environment
- Compare with canonical prod snapshot
- Report any differences
- Auto-fix option for repair

---

## Implementation Checklist

### Phase 1: Schema Export & Compare

- [ ] Create `schema-compare.ts` tool
- [ ] Add `just schema-export` command
- [ ] Add `just schema-diff SNAPSHOT` command
- [ ] Add `just schema-validate` command

### Phase 2: Testing

- [ ] Test migration on fresh DB (empty)
- [ ] Test migration on existing prod snapshot
- [ ] Verify indices are created
- [ ] Verify schema matches prod exactly

### Phase 3: Documentation

- [ ] Add schema verification to CI/CD
- [ ] Document schema sync workflow
- [ ] Add troubleshooting guide

---

## Schema Components to Validate

### Collections

- Name & ID
- Type (auth vs base)
- Field definitions
- Field types and constraints

### Indices

- Index names
- Index columns
- Index type (unique, composite, etc.)

### Auth Configuration (for auth collections)

- OAuth2 settings
- Password auth settings
- MFA/OTP settings
- Token durations
- Email templates

### Rules

- List, View, Create, Update, Delete rules
- Auth rules

### System Settings

- Cron jobs
- Hooks configuration
- File storage settings

---

## Commands

```bash
# Export current schema
just schema-export > schemas_archive/local_$(date +%Y%m%d).json

# Compare with prod
just schema-diff schemas_archive/prod_snapshot_jan_2025.json

# Validate current environment matches prod
just schema-validate

# Fix any differences (apply migrations)
just migrate
```

---

## Expected Outcomes

1. **Deterministic Schema**: Same migration files → identical schema
2. **Easy Verification**: Quick command to check if schema matches prod
3. **Auto-Repair**: Migrations can fix drift
4. **CI Integration**: Schema validation in deployment pipeline
